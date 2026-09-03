package com.odde.donut.services;

import com.odde.donut.algorithms.RecallSilentPeriodTargetSelector;
import com.odde.donut.algorithms.RecallSilentWindowDueInstant;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.MemoryTrackerRepository;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.entities.repositories.UserRepository;
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

  private boolean isUserOverdueForBatch(User user, Timestamp currentTime, Timestamp windowStart) {
    return dueInstantForUser(user, currentTime, windowStart)
        .flatMap(
            dueInstant ->
                latestAcceptedBatchForUser(user)
                    .map(
                        latestAccepted ->
                            isSubmissionBeforeDueInstant(
                                latestAccepted.getSubmittedAt(), dueInstant))
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
    Optional<QuestionGenerationBatch> latestAccepted = latestAcceptedBatchForUser(user);
    if (latestAccepted.isEmpty()
        || isSubmissionBeforeDueInstant(latestAccepted.get().getSubmittedAt(), dueInstant.get())) {
      return false;
    }
    return QuestionGenerationBatchStatus.openAiFailureRetryStatuses()
        .contains(latestAccepted.get().getStatus());
  }

  private Optional<QuestionGenerationBatch> latestAcceptedBatchForUser(User user) {
    return batchRepository.findFirstByUser_IdAndSubmittedAtIsNotNullOrderBySubmittedAtDescIdDesc(
        user.getId());
  }
}
