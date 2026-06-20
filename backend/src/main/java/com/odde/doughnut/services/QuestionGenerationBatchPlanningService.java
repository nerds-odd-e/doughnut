package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.RecallSilentPeriodTargetSelector;
import com.odde.doughnut.algorithms.RecallSilentWindowDueInstant;
import com.odde.doughnut.controllers.dto.QuestionGenerationBatchUserScheduleDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchPlanningService {
  private static final long RECENT_RECALL_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(7);
  private static final long CANDIDATE_TRACKER_WINDOW_MILLIS = TimeUnit.HOURS.toMillis(48);
  private static final int MAX_SCHEDULE_SCAN_HOURS = 7 * 24;

  public static final String REASON_BATCH_IN_PROGRESS = "BATCH_IN_PROGRESS";
  public static final String REASON_NO_RECENT_RECALLS = "NO_RECENT_RECALLS";
  public static final String REASON_NO_CANDIDATE_TRACKERS = "NO_CANDIDATE_TRACKERS";
  public static final String REASON_NO_SCHEDULED_TIME = "NO_SCHEDULED_TIME";

  private final QuestionGenerationBatchRepository batchRepository;
  private final QuestionGenerationBatchRequestRepository batchRequestRepository;
  private final RecallPromptRepository recallPromptRepository;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final UserRepository userRepository;

  public QuestionGenerationBatchPlanningService(
      QuestionGenerationBatchRepository batchRepository,
      QuestionGenerationBatchRequestRepository batchRequestRepository,
      RecallPromptRepository recallPromptRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      UserRepository userRepository) {
    this.batchRepository = batchRepository;
    this.batchRequestRepository = batchRequestRepository;
    this.recallPromptRepository = recallPromptRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.userRepository = userRepository;
  }

  public List<User> findUsersEligibleForBatchSubmission(Timestamp currentTime) {
    Timestamp windowStart = new Timestamp(currentTime.getTime() - RECENT_RECALL_WINDOW_MILLIS);
    return findUsersWithRecentRecallActivity(windowStart, currentTime).stream()
        .filter(user -> isUserEligibleForNewBatchSubmission(user))
        .filter(
            user ->
                isUserOverdueForBatch(user, currentTime, windowStart)
                    || isUserEligibleViaOpenAiFailureRetryPath(user, currentTime, windowStart))
        .toList();
  }

  public List<User> findUsersEligibleForManualBatchSubmission(Timestamp currentTime) {
    Timestamp windowStart = new Timestamp(currentTime.getTime() - RECENT_RECALL_WINDOW_MILLIS);
    return findUsersWithRecentRecallActivity(windowStart, currentTime).stream()
        .filter(user -> isUserEligibleForNewBatchSubmission(user))
        .toList();
  }

  private List<User> findUsersWithRecentRecallActivity(Timestamp windowStart, Timestamp endTime) {
    return recallPromptRepository
        .findUserIdsWithAnsweredRecallsInTimeRange(windowStart, endTime)
        .stream()
        .map(userRepository::findById)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<MemoryTracker> findCandidateMemoryTrackersForBatchGeneration(
      User user, Timestamp currentTime) {
    Timestamp dueBy = new Timestamp(currentTime.getTime() + CANDIDATE_TRACKER_WINDOW_MILLIS);
    return memoryTrackerRepository.findBatchQuestionGenerationCandidatesByUser(user.getId(), dueBy);
  }

  public Optional<QuestionGenerationBatch> planLocalBatchForUser(User user, Timestamp currentTime) {
    List<MemoryTracker> candidates =
        findCandidateMemoryTrackersForBatchGeneration(user, currentTime);
    if (candidates.isEmpty()) {
      return Optional.empty();
    }

    QuestionGenerationBatch batch = new QuestionGenerationBatch();
    batch.setUser(user);
    batch.setStatus(QuestionGenerationBatchStatus.PLANNED);
    batch.setPlannedAt(currentTime);
    final QuestionGenerationBatch savedBatch = batchRepository.saveAndFlush(batch);
    final Integer batchId = savedBatch.getId();
    List<QuestionGenerationBatchRequest> requests =
        candidates.stream()
            .map(
                tracker -> {
                  QuestionGenerationBatchRequest request = new QuestionGenerationBatchRequest();
                  request.setBatch(savedBatch);
                  request.setMemoryTracker(tracker);
                  request.setContextSeed(ThreadLocalRandom.current().nextLong());
                  request.setCustomId(
                      QuestionGenerationBatchRequest.customIdFor(batchId, tracker.getId()));
                  return request;
                })
            .toList();
    batchRequestRepository.saveAll(requests);

    return Optional.of(savedBatch);
  }

  public QuestionGenerationBatchUserScheduleDTO getNextBatchQuestionSchedule(
      User user, Timestamp currentTime) {
    QuestionGenerationBatchUserScheduleDTO dto = new QuestionGenerationBatchUserScheduleDTO();
    if (batchRepository.existsByUser_IdAndStatus(
        user.getId(), QuestionGenerationBatchStatus.SUBMITTED)) {
      dto.setReason(REASON_BATCH_IN_PROGRESS);
      return dto;
    }

    if (!hasRecentRecallActivity(user, currentTime)) {
      dto.setReason(REASON_NO_RECENT_RECALLS);
      return dto;
    }

    boolean sawEligibleTimeWithoutCandidate = false;
    Timestamp candidateTime = nextSchedulerTimeAtOrAfter(currentTime);
    for (int hours = 0; hours <= MAX_SCHEDULE_SCAN_HOURS; hours++) {
      if (isUserEligibleForBatchSchedulingAt(user, candidateTime)) {
        if (!findCandidateMemoryTrackersForBatchGeneration(user, candidateTime).isEmpty()) {
          dto.setNextScheduledAt(candidateTime);
          return dto;
        }
        sawEligibleTimeWithoutCandidate = true;
      }
      candidateTime = new Timestamp(candidateTime.getTime() + TimeUnit.HOURS.toMillis(1));
    }

    dto.setReason(
        sawEligibleTimeWithoutCandidate ? REASON_NO_CANDIDATE_TRACKERS : REASON_NO_SCHEDULED_TIME);
    return dto;
  }

  private boolean isUserOverdueForBatch(User user, Timestamp currentTime, Timestamp windowStart) {
    return dueInstantForUser(user, currentTime, windowStart)
        .flatMap(
            dueInstant ->
                latestSubmittedAtForUser(user)
                    .map(lastSubmission -> isSubmissionBeforeDueInstant(lastSubmission, dueInstant))
                    .or(() -> Optional.of(true)))
        .orElse(false);
  }

  private Optional<LocalDateTime> dueInstantForUser(
      User user, Timestamp currentTime, Timestamp windowStart) {
    List<Timestamp> answerTimestamps =
        recallPromptRepository
            .findAnsweredRecallPromptsInTimeRange(user.getId(), windowStart, currentTime)
            .stream()
            .map(RecallPrompt::getAnswerTime)
            .filter(Objects::nonNull)
            .toList();
    if (answerTimestamps.isEmpty()) {
      return Optional.empty();
    }
    LocalTime targetTimeOfDay =
        RecallSilentPeriodTargetSelector.targetTimeOfDayFromTimestamps(answerTimestamps);
    return Optional.of(
        RecallSilentWindowDueInstant.lastDueInstantAtOrBefore(
            targetTimeOfDay, currentTime.toLocalDateTime()));
  }

  private Optional<Timestamp> latestSubmittedAtForUser(User user) {
    return batchRepository.findLatestSubmittedAtByUser_Id(user.getId());
  }

  private boolean hasRecentRecallActivity(User user, Timestamp currentTime) {
    Timestamp windowStart = new Timestamp(currentTime.getTime() - RECENT_RECALL_WINDOW_MILLIS);
    return !recallPromptRepository
        .findAnsweredRecallPromptsInTimeRange(user.getId(), windowStart, currentTime)
        .isEmpty();
  }

  private boolean isUserEligibleForBatchSchedulingAt(User user, Timestamp candidateTime) {
    Timestamp windowStart = new Timestamp(candidateTime.getTime() - RECENT_RECALL_WINDOW_MILLIS);
    if (recallPromptRepository
        .findAnsweredRecallPromptsInTimeRange(user.getId(), windowStart, candidateTime)
        .isEmpty()) {
      return false;
    }
    if (!isUserEligibleForNewBatchSubmission(user)) {
      return false;
    }
    return isUserOverdueForBatch(user, candidateTime, windowStart)
        || isUserEligibleViaOpenAiFailureRetryPath(user, candidateTime, windowStart);
  }

  public boolean isUserEligibleForNewBatchSubmission(User user) {
    return !batchRepository.existsByUser_IdAndStatus(
        user.getId(), QuestionGenerationBatchStatus.SUBMITTED);
  }

  private static boolean isSubmissionBeforeDueInstant(
      Timestamp lastSubmission, LocalDateTime dueInstant) {
    return lastSubmission.toLocalDateTime().isBefore(dueInstant);
  }

  private boolean isUserEligibleViaOpenAiFailureRetryPath(
      User user, Timestamp currentTime, Timestamp windowStart) {
    Optional<LocalDateTime> dueInstant = dueInstantForUser(user, currentTime, windowStart);
    if (dueInstant.isEmpty()) {
      return false;
    }
    Optional<Timestamp> lastSubmission = latestSubmittedAtForUser(user);
    if (lastSubmission.isEmpty()
        || isSubmissionBeforeDueInstant(lastSubmission.get(), dueInstant.get())) {
      return false;
    }
    return batchRepository.existsByUser_IdAndOpenaiBatchIdIsNotNullAndStatusIn(
        user.getId(), QuestionGenerationBatchStatus.openAiFailureRetryStatuses());
  }

  private Timestamp nextSchedulerTimeAtOrAfter(Timestamp currentTime) {
    long millis = currentTime.getTime();
    long hourMillis = TimeUnit.HOURS.toMillis(1);
    long nextHourMillis = ((millis + hourMillis - 1) / hourMillis) * hourMillis;
    return new Timestamp(nextHourMillis);
  }
}
