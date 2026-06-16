package com.odde.doughnut.services;

import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
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
  private final OpenAiApiHandler openAiApiHandler;
  private final QuestionGenerationBatchMetrics batchMetrics;

  public QuestionGenerationBatchPollingService(
      QuestionGenerationBatchRepository batchRepository,
      OpenAiApiHandler openAiApiHandler,
      QuestionGenerationBatchMetrics batchMetrics) {
    this.batchRepository = batchRepository;
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
      }
    }

    logger.info(
        "Question generation batch polling finished: {} updated, {} unchanged, {} failed",
        updatedCount,
        unchangedCount,
        failedCount);
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
