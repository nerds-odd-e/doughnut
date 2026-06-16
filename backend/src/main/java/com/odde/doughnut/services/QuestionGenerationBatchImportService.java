package com.odde.doughnut.services;

import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import java.sql.Timestamp;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchImportService {
  private static final Logger logger =
      LoggerFactory.getLogger(QuestionGenerationBatchImportService.class);

  private final QuestionGenerationBatchRepository batchRepository;
  private final QuestionGenerationBatchRequestRepository batchRequestRepository;
  private final QuestionGenerationBatchRowImportService rowImportService;
  private final QuestionGenerationBatchMetrics batchMetrics;

  public QuestionGenerationBatchImportService(
      QuestionGenerationBatchRepository batchRepository,
      QuestionGenerationBatchRequestRepository batchRequestRepository,
      QuestionGenerationBatchRowImportService rowImportService,
      QuestionGenerationBatchMetrics batchMetrics) {
    this.batchRepository = batchRepository;
    this.batchRequestRepository = batchRequestRepository;
    this.rowImportService = rowImportService;
    this.batchMetrics = batchMetrics;
  }

  public void importCompletedBatches(Timestamp importedAt) {
    List<QuestionGenerationBatch> importableBatches =
        batchRepository.findByStatusAndOutputCollectedAtIsNotNullAndImportedAtIsNull(
            QuestionGenerationBatchStatus.COMPLETED);
    logger.info(
        "Importing {} completed question generation batches with collected output",
        importableBatches.size());

    int importedCount = 0;
    int failedCount = 0;

    for (QuestionGenerationBatch batch : importableBatches) {
      try {
        if (importBatch(batch, importedAt)) {
          importedCount++;
        }
      } catch (RuntimeException e) {
        failedCount++;
        logger.warn("Failed to import question generation batch {}", batch.getId(), e);
      }
    }

    logger.info(
        "Question generation batch import finished: {} imported, {} failed",
        importedCount,
        failedCount);
  }

  private boolean importBatch(QuestionGenerationBatch batch, Timestamp importedAt) {
    List<QuestionGenerationBatchRequest> requests =
        batchRequestRepository.findByBatch_Id(batch.getId());

    for (QuestionGenerationBatchRequest request : requests) {
      importRequestRow(request);
    }

    List<QuestionGenerationBatchRequest> reloadedRequests =
        batchRequestRepository.findByBatch_Id(batch.getId());
    if (!allRowsImportComplete(reloadedRequests)) {
      return false;
    }

    batch.setImportedAt(importedAt);
    batchRepository.saveAndFlush(batch);
    batchMetrics.recordImportedBatch();
    return true;
  }

  private void importRequestRow(QuestionGenerationBatchRequest request) {
    try {
      rowImportService.importRow(request);
    } catch (RuntimeException e) {
      logger.warn("Failed to import question generation batch request {}", request.getId(), e);
      QuestionGenerationBatchRequest reloadedRequest =
          batchRequestRepository.findById(request.getId()).orElseThrow();
      if (reloadedRequest.getStatus() == QuestionGenerationBatchRequestStatus.OUTPUT_READY) {
        reloadedRequest.setStatus(QuestionGenerationBatchRequestStatus.FAILED);
        reloadedRequest.setErrorDetail("import failed: " + e.getMessage());
        batchRequestRepository.save(reloadedRequest);
        batchMetrics.recordFailedRow();
      }
    }
  }

  private boolean allRowsImportComplete(List<QuestionGenerationBatchRequest> requests) {
    return requests.stream()
        .allMatch(
            request ->
                request.getStatus() == QuestionGenerationBatchRequestStatus.IMPORTED
                    || request.getStatus() == QuestionGenerationBatchRequestStatus.FAILED);
  }
}
