package com.odde.doughnut.services;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchUserStateRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchPlanningService {
  private static final long SUBMISSION_GATE_MILLIS = TimeUnit.HOURS.toMillis(23);
  private static final long RECENT_RECALL_WINDOW_MILLIS = TimeUnit.DAYS.toMillis(7);

  private final QuestionGenerationBatchUserStateRepository userStateRepository;
  private final RecallPromptRepository recallPromptRepository;
  private final UserRepository userRepository;

  public QuestionGenerationBatchPlanningService(
      QuestionGenerationBatchUserStateRepository userStateRepository,
      RecallPromptRepository recallPromptRepository,
      UserRepository userRepository) {
    this.userStateRepository = userStateRepository;
    this.recallPromptRepository = recallPromptRepository;
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
        .toList();
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
