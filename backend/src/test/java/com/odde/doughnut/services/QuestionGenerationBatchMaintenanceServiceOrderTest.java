package com.odde.doughnut.services;

import static org.mockito.Mockito.inOrder;

import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionGenerationBatchMaintenanceServiceOrderTest {

  @Mock QuestionGenerationBatchPollingService pollingService;
  @Mock QuestionGenerationBatchOutputCollectionService outputCollectionService;
  @Mock QuestionGenerationBatchImportService batchImportService;
  @Mock QuestionGenerationBatchRetentionService retentionService;

  QuestionGenerationBatchMaintenanceService maintenanceService;

  @BeforeEach
  void setup() {
    maintenanceService =
        new QuestionGenerationBatchMaintenanceService(
            pollingService, outputCollectionService, batchImportService, retentionService);
  }

  @Test
  void shouldPruneTerminalBatchesAfterImport() {
    Timestamp currentTime = new Timestamp(System.currentTimeMillis());

    maintenanceService.resumeExistingBatches(currentTime);

    InOrder inOrder =
        inOrder(pollingService, outputCollectionService, batchImportService, retentionService);
    inOrder.verify(pollingService).pollSubmittedBatches();
    inOrder.verify(outputCollectionService).collectOutputForCompletedBatches(currentTime);
    inOrder.verify(batchImportService).importCompletedBatches(currentTime);
    inOrder.verify(retentionService).pruneTerminalBatches(currentTime);
  }
}
