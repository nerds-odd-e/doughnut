package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

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
  QuestionGenerationBatchMaintenanceRunState maintenanceRunState;

  QuestionGenerationBatchMaintenanceJob job;

  @BeforeEach
  void setup() {
    maintenanceRunState = new QuestionGenerationBatchMaintenanceRunState();
    job =
        new QuestionGenerationBatchMaintenanceJob(
            maintenanceService, submitDueUsersService, maintenanceRunState);
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
  void recordsStartedAndFinishedTimestamps() {
    job.runHourlyMaintenance();

    assertThat(maintenanceRunState.getLastMaintenanceStartedAt(), notNullValue());
    assertThat(maintenanceRunState.getLastMaintenanceFinishedAt(), notNullValue());
  }

  @Test
  void recordsResumeErrorWhenResumeFailsAndStillSubmitsDueUsers() {
    doThrow(new RuntimeException("resume failed"))
        .when(maintenanceService)
        .resumeExistingBatches(any(Timestamp.class));

    job.runHourlyMaintenance();

    verify(submitDueUsersService).submitDueUsers(any(Timestamp.class));
    assertThat(maintenanceRunState.getLastMaintenanceError(), containsString("resume failed"));
  }

  @Test
  void recordsSubmissionErrorWhenSubmitFails() {
    doAnswer(
            invocation -> {
              throw new RuntimeException("submit failed");
            })
        .when(submitDueUsersService)
        .submitDueUsers(any(Timestamp.class));

    assertThrows(RuntimeException.class, () -> job.runHourlyMaintenance());

    assertThat(maintenanceRunState.getLastMaintenanceError(), containsString("submit failed"));
    assertThat(maintenanceRunState.getLastMaintenanceFinishedAt(), notNullValue());
  }
}
