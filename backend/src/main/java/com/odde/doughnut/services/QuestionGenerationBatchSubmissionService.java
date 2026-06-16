package com.odde.doughnut.services;

import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchUserState;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchUserStateRepository;
import com.odde.doughnut.services.QuestionGenerationBatchUserSubmissionTx.DueUserSubmissionOutcome;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchSubmissionService {
  private static final Logger logger =
      LoggerFactory.getLogger(QuestionGenerationBatchSubmissionService.class);

  private final QuestionGenerationBatchJsonlRenderer jsonlRenderer;
  private final OpenAiApiHandler openAiApiHandler;
  private final QuestionGenerationBatchRepository batchRepository;
  private final QuestionGenerationBatchUserStateRepository userStateRepository;
  private final QuestionGenerationBatchPlanningService planningService;
  private final QuestionGenerationBatchUserSubmissionTx userSubmissionTx;

  public QuestionGenerationBatchSubmissionService(
      QuestionGenerationBatchJsonlRenderer jsonlRenderer,
      OpenAiApiHandler openAiApiHandler,
      QuestionGenerationBatchRepository batchRepository,
      QuestionGenerationBatchUserStateRepository userStateRepository,
      QuestionGenerationBatchPlanningService planningService,
      @Lazy QuestionGenerationBatchUserSubmissionTx userSubmissionTx) {
    this.jsonlRenderer = jsonlRenderer;
    this.openAiApiHandler = openAiApiHandler;
    this.batchRepository = batchRepository;
    this.userStateRepository = userStateRepository;
    this.planningService = planningService;
    this.userSubmissionTx = userSubmissionTx;
  }

  public void submitDueUsers(Timestamp currentTime) {
    List<User> dueUsers = planningService.findUsersEligibleForBatchSubmission(currentTime);
    logger.info("Submitting question generation batches for {} due users", dueUsers.size());

    int submittedCount = 0;
    int failedCount = 0;
    int skippedCount = 0;

    for (User user : dueUsers) {
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
  }

  public boolean submitPlannedBatch(QuestionGenerationBatch batch, Timestamp submissionTime) {
    if (batch.getStatus() != QuestionGenerationBatchStatus.PLANNED) {
      throw new IllegalStateException(
          "Only planned batches can be submitted, but batch "
              + batch.getId()
              + " has status "
              + batch.getStatus());
    }

    try {
      String jsonl = jsonlRenderer.renderInputJsonl(batch);
      String inputFileId =
          openAiApiHandler.uploadBatchInputFile(jsonl.getBytes(StandardCharsets.UTF_8));
      String openAiBatchId = openAiApiHandler.createResponsesBatch(inputFileId);

      batch.setOpenaiInputFileId(inputFileId);
      batch.setOpenaiBatchId(openAiBatchId);
      batch.setSubmittedAt(submissionTime);
      batch.setStatus(QuestionGenerationBatchStatus.SUBMITTED);
      batchRepository.saveAndFlush(batch);

      recordSuccessfulSubmission(batch.getUser(), submissionTime);
      return true;
    } catch (RuntimeException e) {
      batch.setStatus(QuestionGenerationBatchStatus.FAILED);
      batchRepository.saveAndFlush(batch);
      return false;
    }
  }

  private void recordSuccessfulSubmission(User user, Timestamp submittedAt) {
    QuestionGenerationBatchUserState state =
        userStateRepository
            .findByUser_Id(user.getId())
            .orElseGet(
                () -> {
                  QuestionGenerationBatchUserState newState =
                      new QuestionGenerationBatchUserState();
                  newState.setUser(user);
                  return newState;
                });
    state.setLastSuccessfulSubmittedAt(submittedAt);
    userStateRepository.saveAndFlush(state);
  }
}
