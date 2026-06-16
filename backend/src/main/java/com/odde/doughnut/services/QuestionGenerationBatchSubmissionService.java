package com.odde.doughnut.services;

import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchUserState;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchUserStateRepository;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchSubmissionService {
  private final QuestionGenerationBatchJsonlRenderer jsonlRenderer;
  private final OpenAiApiHandler openAiApiHandler;
  private final QuestionGenerationBatchRepository batchRepository;
  private final QuestionGenerationBatchUserStateRepository userStateRepository;
  private final QuestionGenerationBatchMetrics batchMetrics;

  public QuestionGenerationBatchSubmissionService(
      QuestionGenerationBatchJsonlRenderer jsonlRenderer,
      OpenAiApiHandler openAiApiHandler,
      QuestionGenerationBatchRepository batchRepository,
      QuestionGenerationBatchUserStateRepository userStateRepository,
      QuestionGenerationBatchMetrics batchMetrics) {
    this.jsonlRenderer = jsonlRenderer;
    this.openAiApiHandler = openAiApiHandler;
    this.batchRepository = batchRepository;
    this.userStateRepository = userStateRepository;
    this.batchMetrics = batchMetrics;
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
      batchMetrics.recordSubmittedBatch();
      return true;
    } catch (RuntimeException e) {
      batch.setStatus(QuestionGenerationBatchStatus.FAILED);
      batchRepository.saveAndFlush(batch);
      batchMetrics.recordFailedBatch();
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
