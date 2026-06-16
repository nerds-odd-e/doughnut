package com.odde.doughnut.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;

import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionGenerationBatchMaintenanceJobTests {

  @Mock QuestionGenerationBatchMaintenanceService maintenanceService;
  @Mock QuestionGenerationBatchSubmissionService submissionService;

  QuestionGenerationBatchMaintenanceJob job;

  @BeforeEach
  void setup() {
    job = new QuestionGenerationBatchMaintenanceJob(maintenanceService, submissionService);
  }

  @Test
  void shouldResumeExistingBatchesBeforeSubmittingDueUsers() {
    job.runHourlyMaintenance();

    InOrder inOrder = inOrder(maintenanceService, submissionService);
    inOrder.verify(maintenanceService).resumeExistingBatches(any(Timestamp.class));
    inOrder.verify(submissionService).submitDueUsers(any(Timestamp.class));
  }
}
