package com.odde.doughnut.services;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchUserStateRepository;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchPlanningService {
  private static final long SUBMISSION_GATE_MILLIS = TimeUnit.HOURS.toMillis(23);

  private final QuestionGenerationBatchUserStateRepository userStateRepository;

  public QuestionGenerationBatchPlanningService(
      QuestionGenerationBatchUserStateRepository userStateRepository) {
    this.userStateRepository = userStateRepository;
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
