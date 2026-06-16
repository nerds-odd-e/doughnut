package com.odde.doughnut.services;

import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchMaintenanceService {
  private final QuestionGenerationBatchPollingService pollingService;
  private final QuestionGenerationBatchOutputCollectionService outputCollectionService;
  private final QuestionGenerationBatchImportService batchImportService;
  private final QuestionGenerationBatchRetentionService retentionService;

  public QuestionGenerationBatchMaintenanceService(
      QuestionGenerationBatchPollingService pollingService,
      QuestionGenerationBatchOutputCollectionService outputCollectionService,
      QuestionGenerationBatchImportService batchImportService,
      QuestionGenerationBatchRetentionService retentionService) {
    this.pollingService = pollingService;
    this.outputCollectionService = outputCollectionService;
    this.batchImportService = batchImportService;
    this.retentionService = retentionService;
  }

  public void resumeExistingBatches(Timestamp currentTime) {
    pollingService.pollSubmittedBatches();
    outputCollectionService.collectOutputForCompletedBatches(currentTime);
    batchImportService.importCompletedBatches(currentTime);
    retentionService.pruneTerminalBatches(currentTime);
  }
}
