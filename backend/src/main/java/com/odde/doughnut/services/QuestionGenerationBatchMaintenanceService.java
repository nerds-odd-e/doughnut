package com.odde.doughnut.services;

import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchMaintenanceService {
  private final QuestionGenerationBatchPollingService pollingService;
  private final QuestionGenerationBatchOutputCollectionService outputCollectionService;
  private final QuestionGenerationBatchImportService batchImportService;

  public QuestionGenerationBatchMaintenanceService(
      QuestionGenerationBatchPollingService pollingService,
      QuestionGenerationBatchOutputCollectionService outputCollectionService,
      QuestionGenerationBatchImportService batchImportService) {
    this.pollingService = pollingService;
    this.outputCollectionService = outputCollectionService;
    this.batchImportService = batchImportService;
  }

  public void resumeExistingBatches(Timestamp currentTime) {
    pollingService.pollSubmittedBatches();
    outputCollectionService.collectOutputForCompletedBatches(currentTime);
    batchImportService.importCompletedBatches(currentTime);
  }
}
