package com.odde.donut.services;

import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchSubmissionService {
  private final QuestionGenerationBatchJsonlRenderer jsonlRenderer;
  private final OpenAiApiHandler openAiApiHandler;
  private final QuestionGenerationBatchRepository batchRepository;
  private final QuestionGenerationBatchMetrics batchMetrics;
  private final QuestionGenerationBatchSubmissionFailureTx submissionFailureTx;

  public QuestionGenerationBatchSubmissionService(
      QuestionGenerationBatchJsonlRenderer jsonlRenderer,
      OpenAiApiHandler openAiApiHandler,
      QuestionGenerationBatchRepository batchRepository,
      QuestionGenerationBatchMetrics batchMetrics,
      QuestionGenerationBatchSubmissionFailureTx submissionFailureTx) {
    this.jsonlRenderer = jsonlRenderer;
    this.openAiApiHandler = openAiApiHandler;
    this.batchRepository = batchRepository;
    this.batchMetrics = batchMetrics;
    this.submissionFailureTx = submissionFailureTx;
  }

  public void submitPlannedBatch(QuestionGenerationBatch batch, Timestamp submissionTime) {
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

      batchMetrics.recordSubmittedBatch();
    } catch (RuntimeException e) {
      submissionFailureTx.persistFailedSubmission(batch, e.getMessage());
      batchMetrics.recordFailedBatch();
      throw e;
    }
  }
}
