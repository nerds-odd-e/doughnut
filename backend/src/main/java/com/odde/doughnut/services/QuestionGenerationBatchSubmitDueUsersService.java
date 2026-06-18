package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchSubmissionSummaryDTO;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.QuestionGenerationBatchUserSubmissionTx.DueUserSubmissionOutcome;
import java.sql.Timestamp;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchSubmitDueUsersService {
  private static final Logger logger =
      LoggerFactory.getLogger(QuestionGenerationBatchSubmitDueUsersService.class);

  private final QuestionGenerationBatchPlanningService planningService;
  private final QuestionGenerationBatchUserSubmissionTx userSubmissionTx;

  public QuestionGenerationBatchSubmitDueUsersService(
      QuestionGenerationBatchPlanningService planningService,
      QuestionGenerationBatchUserSubmissionTx userSubmissionTx) {
    this.planningService = planningService;
    this.userSubmissionTx = userSubmissionTx;
  }

  public QuestionGenerationBatchSubmissionSummaryDTO submitDueUsers(Timestamp currentTime) {
    List<User> dueUsers = planningService.findUsersEligibleForBatchSubmission(currentTime);
    return submitUsers(dueUsers, currentTime);
  }

  public QuestionGenerationBatchSubmissionSummaryDTO submitUsersWithRecentRecalls(
      Timestamp currentTime) {
    List<User> users = planningService.findUsersEligibleForManualBatchSubmission(currentTime);
    return submitUsers(users, currentTime);
  }

  private QuestionGenerationBatchSubmissionSummaryDTO submitUsers(
      List<User> users, Timestamp currentTime) {
    logger.info("Submitting question generation batches for {} users", users.size());

    int submittedCount = 0;
    int failedCount = 0;
    int skippedCount = 0;

    for (User user : users) {
      try {
        DueUserSubmissionOutcome outcome = userSubmissionTx.processDueUser(user, currentTime);
        switch (outcome.kind()) {
          case SKIPPED -> {
            skippedCount++;
            logger.info(
                "Skipped question generation batch for user {} with no candidate trackers",
                outcome.userId());
          }
          case SUBMITTED -> {
            submittedCount++;
            logger.info(
                "Submitted question generation batch {} for user {} with OpenAI batch id {}",
                outcome.localBatchId(),
                outcome.userId(),
                outcome.openAiBatchId());
          }
          case FAILED -> {
            failedCount++;
            logger.info(
                "Failed question generation batch {} for user {}",
                outcome.localBatchId(),
                outcome.userId());
          }
        }
      } catch (RuntimeException e) {
        failedCount++;
        logger.warn(
            "Unexpected failure while submitting question generation batch for user {}",
            user.getId(),
            e);
      }
    }

    logger.info(
        "Question generation batch submission finished: {} submitted, {} failed, {} skipped",
        submittedCount,
        failedCount,
        skippedCount);
    return new QuestionGenerationBatchSubmissionSummaryDTO(
        users.size(), submittedCount, failedCount, skippedCount);
  }
}
