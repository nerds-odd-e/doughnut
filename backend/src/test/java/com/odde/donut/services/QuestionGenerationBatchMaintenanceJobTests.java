package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.odde.donut.entities.FailureReport;
import com.odde.donut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
import com.odde.donut.entities.repositories.FailureReportRepository;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionGenerationBatchMaintenanceJobTests {

  @Mock QuestionGenerationBatchMaintenanceService maintenanceService;
  @Mock QuestionGenerationBatchSubmitDueUsersService submitDueUsersService;
  @Mock QuestionGenerationBatchMaintenanceRunService maintenanceRunService;
  @Mock GithubService githubService;
  @Mock FailureReportRepository failureReportRepository;

  QuestionGenerationBatchMaintenanceJob job;

  @BeforeEach
  void setup() {
    job =
        new QuestionGenerationBatchMaintenanceJob(
            maintenanceService,
            submitDueUsersService,
            maintenanceRunService,
            githubService,
            failureReportRepository);
  }

  @Nested
  class ResumeExistingBatchesInvocationOrder {
    @Mock QuestionGenerationBatchPollingService pollingService;
    @Mock QuestionGenerationBatchOutputCollectionService outputCollectionService;
    @Mock QuestionGenerationBatchImportService batchImportService;
    @Mock QuestionGenerationBatchRetentionService retentionService;

    @Test
    void shouldPruneTerminalBatchesAfterImport() {
      QuestionGenerationBatchMaintenanceService batchMaintenanceService =
          new QuestionGenerationBatchMaintenanceService(
              pollingService, outputCollectionService, batchImportService, retentionService);
      Timestamp currentTime = new Timestamp(System.currentTimeMillis());

      batchMaintenanceService.resumeExistingBatches(currentTime);

      InOrder inOrder =
          inOrder(pollingService, outputCollectionService, batchImportService, retentionService);
      inOrder.verify(pollingService).pollSubmittedBatches();
      inOrder.verify(outputCollectionService).collectOutputForCompletedBatches(currentTime);
      inOrder.verify(batchImportService).importCompletedBatches(currentTime);
      inOrder.verify(retentionService).pruneTerminalBatches(currentTime);
    }
  }

  @Test
  void shouldResumeExistingBatchesBeforeSubmittingDueUsers() {
    job.runHourlyMaintenance();

    InOrder inOrder = inOrder(maintenanceService, submitDueUsersService);
    inOrder.verify(maintenanceService).resumeExistingBatches(any(Timestamp.class));
    inOrder.verify(submitDueUsersService).submitDueUsers(any(Timestamp.class));
  }

  @Test
  void recordsStartedAndFinishedTimestampsForScheduledRuns() {
    job.runHourlyMaintenance();

    verify(maintenanceRunService)
        .recordStarted(eq(QuestionGenerationBatchMaintenanceTriggerSource.SCHEDULED), any());
    verify(maintenanceRunService).recordFinished(any(Timestamp.class));
  }

  @Test
  void recordsResumeErrorAndStillSubmitsDueUsersWhenResumeFails() {
    doThrow(new RuntimeException("resume failed"))
        .when(maintenanceService)
        .resumeExistingBatches(any(Timestamp.class));

    job.runHourlyMaintenance();

    verify(maintenanceRunService).recordError(any(RuntimeException.class));
    verify(submitDueUsersService).submitDueUsers(any(Timestamp.class));

    ArgumentCaptor<FailureReport> reportCaptor = ArgumentCaptor.forClass(FailureReport.class);
    verify(failureReportRepository, atLeastOnce()).save(reportCaptor.capture());
    assertThat(
        reportCaptor.getValue().getErrorDetail(),
        containsString("QuestionGenerationBatchMaintenanceJob"));
  }

  @Test
  void recordsSubmissionErrorWhenSubmitFails() {
    doThrow(new RuntimeException("submit failed"))
        .when(submitDueUsersService)
        .submitDueUsers(any(Timestamp.class));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> job.runHourlyMaintenance());

    assertThat(thrown.getMessage(), containsString("submit failed"));
    verify(maintenanceRunService).recordError(any(RuntimeException.class));
    verify(maintenanceRunService).recordFinished(any(Timestamp.class));
    verify(failureReportRepository, never()).save(any());
  }
}
