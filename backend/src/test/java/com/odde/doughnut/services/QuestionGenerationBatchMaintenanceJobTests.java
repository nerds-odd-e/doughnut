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
  @Mock QuestionGenerationBatchSubmitDueUsersService submitDueUsersService;

  QuestionGenerationBatchMaintenanceJob job;

  @BeforeEach
  void setup() {
    job = new QuestionGenerationBatchMaintenanceJob(maintenanceService, submitDueUsersService);
  }

  @Test
  void shouldResumeExistingBatchesBeforeSubmittingDueUsers() {
    job.runHourlyMaintenance();

    InOrder inOrder = inOrder(maintenanceService, submitDueUsersService);
    inOrder.verify(maintenanceService).resumeExistingBatches(any(Timestamp.class));
    inOrder.verify(submitDueUsersService).submitDueUsers(any(Timestamp.class));
  }
}
