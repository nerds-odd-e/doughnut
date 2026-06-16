package com.odde.doughnut.services;

import com.odde.doughnut.algorithms.CronHourTargetDueSelector;
import com.odde.doughnut.algorithms.RecallSilentPeriodTargetSelector;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.MemoryTrackerRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchUserStateRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchPlanningService {
  private static final long SUBMISSION_GATE_MILLIS = TimeUnit.HOURS.toMillis(23);
  private static final long RECENT_RECALL_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(7);
  private static final long CANDIDATE_TRACKER_WINDOW_MILLIS = TimeUnit.HOURS.toMillis(48);

  private final QuestionGenerationBatchUserStateRepository userStateRepository;
  private final RecallPromptRepository recallPromptRepository;
  private final MemoryTrackerRepository memoryTrackerRepository;
  private final UserRepository userRepository;

  public QuestionGenerationBatchPlanningService(
      QuestionGenerationBatchUserStateRepository userStateRepository,
      RecallPromptRepository recallPromptRepository,
      MemoryTrackerRepository memoryTrackerRepository,
      UserRepository userRepository) {
    this.userStateRepository = userStateRepository;
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
        .filter(user -> isUserPastSubmissionGate(user, currentTime))
        .filter(user -> isUserDueInCurrentCronHour(user, currentTime, windowStart))
        .toList();
  }

  public List<MemoryTracker> findCandidateMemoryTrackersForBatchGeneration(
      User user, Timestamp currentTime) {
    Timestamp dueBy = new Timestamp(currentTime.getTime() + CANDIDATE_TRACKER_WINDOW_MILLIS);
    return memoryTrackerRepository.findBatchQuestionGenerationCandidatesByUser(user.getId(), dueBy);
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

  public boolean isUserPastSubmissionGate(User user, Timestamp currentTime) {
    return userStateRepository
        .findByUser_Id(user.getId())
        .map(state -> isPastGate(state.getLastSuccessfulSubmittedAt(), currentTime))
        .orElse(true);
  }

  private boolean isPastGate(Timestamp lastSubmittedAt, Timestamp currentTime) {
    return currentTime.getTime() - lastSubmittedAt.getTime() >= SUBMISSION_GATE_MILLIS;
  }
}
