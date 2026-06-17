package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.CronHourTargetDueSelector;
import com.odde.doughnut.algorithms.RecallSilentPeriodTargetSelector;
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
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchUserStateRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchPlanningService {
  private static final long SUBMISSION_GATE_MILLIS = TimeUnit.HOURS.toMillis(23);
  private static final long RECENT_RECALL_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(7);
  private static final long CANDIDATE_TRACKER_WINDOW_MILLIS = TimeUnit.HOURS.toMillis(48);
  private static final int MAX_SCHEDULE_SCAN_HOURS = 7 * 24;

  public static final String REASON_BATCH_IN_PROGRESS = "BATCH_IN_PROGRESS";
  public static final String REASON_NO_RECENT_RECALLS = "NO_RECENT_RECALLS";
  public static final String REASON_NO_CANDIDATE_TRACKERS = "NO_CANDIDATE_TRACKERS";
  public static final String REASON_NO_SCHEDULED_TIME = "NO_SCHEDULED_TIME";

  private final QuestionGenerationBatchUserStateRepository userStateRepository;
  private final QuestionGenerationBatchRepository batchRepository;
  private final QuestionGenerationBatchRequestRepository batchRequestRepository;
  private final RecallPromptRepository recallPromptRepository;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final UserRepository userRepository;

  public QuestionGenerationBatchPlanningService(
      QuestionGenerationBatchUserStateRepository userStateRepository,
      QuestionGenerationBatchRepository batchRepository,
      QuestionGenerationBatchRequestRepository batchRequestRepository,
      RecallPromptRepository recallPromptRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      UserRepository userRepository) {
    this.userStateRepository = userStateRepository;
    this.batchRepository = batchRepository;
    this.batchRequestRepository = batchRequestRepository;
    this.recallPromptRepository = recallPromptRepository;
    this.memoryTrackerRepository = memoryTrackerRepository;
    this.userRepository = userRepository;
  }

  public List<User> findUsersEligibleForBatchSubmission(Timestamp currentTime) {
    Timestamp windowStart = new Timestamp(currentTime.getTime() - RECENT_RECALL_WINDOW_MILLIS);
    return recallPromptRepository
        .findUserIdsWithAnsweredRecallsInTimeRange(windowStart, currentTime)
        .stream()
        .map(userRepository::findById)
        .flatMap(Optional::stream)
        .filter(user -> isUserEligibleForNewBatchSubmission(user, currentTime))
        .filter(
            user ->
                isUserEligibleViaOpenAiFailureRetryPath(user, currentTime)
                    || isUserDueInCurrentCronHour(user, currentTime, windowStart))
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

  private boolean isUserDueInCurrentCronHour(
      User user, Timestamp currentTime, Timestamp windowStart) {
    List<Timestamp> answerTimestamps =
        recallPromptRepository
            .findAnsweredRecallPromptsInTimeRange(user.getId(), windowStart, currentTime)
            .stream()
            .map(RecallPrompt::getAnswerTime)
            .filter(Objects::nonNull)
            .toList();
    if (answerTimestamps.isEmpty()) {
      return false;
    }
    LocalTime targetTimeOfDay =
        RecallSilentPeriodTargetSelector.targetTimeOfDayFromTimestamps(answerTimestamps);
    return CronHourTargetDueSelector.isTargetDueInCronHour(targetTimeOfDay, currentTime);
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
    if (!isUserEligibleForNewBatchSubmission(user, candidateTime)) {
      return false;
    }
    return isUserEligibleViaOpenAiFailureRetryPath(user, candidateTime)
        || isUserDueInCurrentCronHour(user, candidateTime, windowStart);
  }

  public boolean isUserPastSubmissionGate(User user, Timestamp currentTime) {
    return userStateRepository
        .findByUser_Id(user.getId())
        .map(state -> isPastGate(state.getLastSuccessfulSubmittedAt(), currentTime))
        .orElse(true);
  }

  public boolean isUserEligibleForNewBatchSubmission(User user, Timestamp currentTime) {
    if (batchRepository.existsByUser_IdAndStatus(
        user.getId(), QuestionGenerationBatchStatus.SUBMITTED)) {
      return false;
    }
    return isUserPastSubmissionGate(user, currentTime)
        || isUserEligibleViaOpenAiFailureRetryPath(user, currentTime);
  }

  private boolean isUserEligibleViaOpenAiFailureRetryPath(User user, Timestamp currentTime) {
    if (isUserPastSubmissionGate(user, currentTime)) {
      return false;
    }
    return batchRepository.existsByUser_IdAndOpenaiBatchIdIsNotNullAndStatusIn(
        user.getId(), QuestionGenerationBatchStatus.openAiFailureRetryStatuses());
  }

  private boolean isPastGate(Timestamp lastSubmittedAt, Timestamp currentTime) {
    return currentTime.getTime() - lastSubmittedAt.getTime() >= SUBMISSION_GATE_MILLIS;
  }

  private Timestamp nextSchedulerTimeAtOrAfter(Timestamp currentTime) {
    long millis = currentTime.getTime();
    long hourMillis = TimeUnit.HOURS.toMillis(1);
    long nextHourMillis = ((millis + hourMillis - 1) / hourMillis) * hourMillis;
    return new Timestamp(nextHourMillis);
  }
}
