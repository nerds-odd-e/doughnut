package com.odde.donut.services;

import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.batches.Batch;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchPollingService {
  private static final Logger logger =
      LoggerFactory.getLogger(QuestionGenerationBatchPollingService.class);

  private final QuestionGenerationBatchRepository batchRepository;
  private final QuestionGenerationBatchRequestRepository batchRequestRepository;
  private final OpenAiApiHandler openAiApiHandler;
  private final QuestionGenerationBatchMetrics batchMetrics;

  public QuestionGenerationBatchPollingService(
      QuestionGenerationBatchRepository batchRepository,
      QuestionGenerationBatchRequestRepository batchRequestRepository,
      OpenAiApiHandler openAiApiHandler,
      QuestionGenerationBatchMetrics batchMetrics) {
    this.batchRepository = batchRepository;
    this.batchRequestRepository = batchRequestRepository;
    this.openAiApiHandler = openAiApiHandler;
    this.batchMetrics = batchMetrics;
  }

  public void pollSubmittedBatches() {
    List<QuestionGenerationBatch> submittedBatches =
        batchRepository.findByStatus(QuestionGenerationBatchStatus.SUBMITTED);
    logger.info(
        "Polling OpenAI status for {} submitted question generation batches",
        submittedBatches.size());

    int updatedCount = 0;
    int unchangedCount = 0;
    int failedCount = 0;
    RuntimeException firstFailure = null;

    for (QuestionGenerationBatch batch : submittedBatches) {
      try {
        if (updateBatchFromOpenAi(batch)) {
          updatedCount++;
        } else {
          unchangedCount++;
        }
      } catch (RuntimeException e) {
        failedCount++;
        logger.warn(
            "Failed to poll OpenAI status for question generation batch {}", batch.getId(), e);
        if (firstFailure == null) {
          firstFailure =
              new RuntimeException(
                  "Failed to poll OpenAI batch " + batch.getOpenaiBatchId() + ": " + e.getMessage(),
                  e);
        }
      }
    }

    logger.info(
        "Question generation batch polling finished: {} updated, {} unchanged, {} failed",
        updatedCount,
        unchangedCount,
        failedCount);
    if (firstFailure != null) {
      throw firstFailure;
    }
  }

  private boolean updateBatchFromOpenAi(QuestionGenerationBatch batch) {
    Batch openAiBatch = openAiApiHandler.retrieveBatch(batch.getOpenaiBatchId());
    Optional<QuestionGenerationBatchStatus> mappedStatus = mapOpenAiStatus(openAiBatch.status());
    if (mappedStatus.isEmpty()) {
      return false;
    }

    QuestionGenerationBatchStatus newStatus = mappedStatus.get();
    batch.setStatus(newStatus);
    if (newStatus == QuestionGenerationBatchStatus.COMPLETED) {
      batch.setOpenaiOutputFileId(openAiBatch.outputFileId().orElse(null));
      batch.setOpenaiErrorFileId(openAiBatch.errorFileId().orElse(null));
    }
    if (QuestionGenerationBatchStatus.openAiFailureRetryStatuses().contains(newStatus)) {
      String errorDetail =
          newStatus == QuestionGenerationBatchStatus.EXPIRED
              ? QuestionGenerationBatchRequest.ERROR_OPENAI_BATCH_EXPIRED
              : QuestionGenerationBatchRequest.ERROR_OPENAI_BATCH_FAILED;
      batchRequestRepository.markPendingAsFailedForBatch(batch.getId(), errorDetail);
    }
    batchRepository.saveAndFlush(batch);
    recordBatchStatusMetric(newStatus);
    return true;
  }

  private void recordBatchStatusMetric(QuestionGenerationBatchStatus status) {
    switch (status) {
      case COMPLETED -> batchMetrics.recordCompletedBatch();
      case FAILED -> batchMetrics.recordFailedBatch();
      case EXPIRED -> batchMetrics.recordExpiredBatch();
      default -> {}
    }
  }

  private Optional<QuestionGenerationBatchStatus> mapOpenAiStatus(Batch.Status openAiStatus) {
    if (openAiStatus.equals(Batch.Status.VALIDATING)
        || openAiStatus.equals(Batch.Status.IN_PROGRESS)
        || openAiStatus.equals(Batch.Status.FINALIZING)) {
      return Optional.empty();
    }
    if (openAiStatus.equals(Batch.Status.COMPLETED)) {
      return Optional.of(QuestionGenerationBatchStatus.COMPLETED);
    }
    if (openAiStatus.equals(Batch.Status.FAILED) || openAiStatus.equals(Batch.Status.CANCELLED)) {
      return Optional.of(QuestionGenerationBatchStatus.FAILED);
    }
    if (openAiStatus.equals(Batch.Status.EXPIRED)) {
      return Optional.of(QuestionGenerationBatchStatus.EXPIRED);
    }
    return Optional.empty();
  }
}
