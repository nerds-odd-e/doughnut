package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
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
  @Mock QuestionGenerationBatchMaintenanceRunService maintenanceRunService;

  QuestionGenerationBatchMaintenanceJob job;

  @BeforeEach
  void setup() {
    job =
        new QuestionGenerationBatchMaintenanceJob(
            maintenanceService, submitDueUsersService, maintenanceRunService);
  }

  @Test
  void shouldResumeExistingBatchesBeforeSubmittingDueUsers() {
    job.runHourlyMaintenance();

    InOrder inOrder = inOrder(maintenanceService, submitDueUsersService);
    inOrder.verify(maintenanceService).resumeExistingBatches(any(Timestamp.class));
    inOrder.verify(submitDueUsersService).submitDueUsers(any(Timestamp.class));
  }

  @Test
  void shouldStillSubmitDueUsersWhenResumeExistingBatchesFails() {
    doThrow(new RuntimeException("resume failed"))
        .when(maintenanceService)
        .resumeExistingBatches(any(Timestamp.class));

    job.runHourlyMaintenance();

    verify(submitDueUsersService).submitDueUsers(any(Timestamp.class));
  }

  @Test
  void recordsStartedAndFinishedTimestampsForScheduledRuns() {
    job.runHourlyMaintenance();

    verify(maintenanceRunService)
        .recordStarted(eq(QuestionGenerationBatchMaintenanceTriggerSource.SCHEDULED), any());
    verify(maintenanceRunService).recordFinished(any(Timestamp.class));
  }

  @Test
  void recordsResumeErrorWhenResumeFailsAndStillSubmitsDueUsers() {
    doThrow(new RuntimeException("resume failed"))
        .when(maintenanceService)
        .resumeExistingBatches(any(Timestamp.class));

    job.runHourlyMaintenance();

    verify(submitDueUsersService).submitDueUsers(any(Timestamp.class));
    verify(maintenanceRunService).recordError(any(RuntimeException.class));
  }

  @Test
  void recordsSubmissionErrorWhenSubmitFails() {
    doAnswer(
            invocation -> {
              throw new RuntimeException("submit failed");
            })
        .when(submitDueUsersService)
        .submitDueUsers(any(Timestamp.class));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> job.runHourlyMaintenance());

    assertThat(thrown.getMessage(), containsString("submit failed"));
    verify(maintenanceRunService).recordError(any(RuntimeException.class));
    verify(maintenanceRunService).recordFinished(any(Timestamp.class));
  }
}
