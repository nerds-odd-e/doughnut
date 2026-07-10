package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchAdminStatusDTO;
import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceRun;
import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;

class QuestionGenerationBatchAdminStatusServiceTest {

  QuestionGenerationBatchRepository batchRepository;
  QuestionGenerationBatchRequestRepository requestRepository;
  StandardEnvironment environment;
  QuestionGenerationBatchMaintenanceRunService maintenanceRunService;

  @BeforeEach
  void setup() {
    batchRepository = mock(QuestionGenerationBatchRepository.class);
    requestRepository = mock(QuestionGenerationBatchRequestRepository.class);
    environment = new StandardEnvironment();
    maintenanceRunService = mock(QuestionGenerationBatchMaintenanceRunService.class);
    when(batchRepository.countByStatus()).thenReturn(List.of());
    when(requestRepository.countByStatus()).thenReturn(List.of());
  }

  @Test
  void reportsBatchAndRequestCountsFromRepositories() {
    when(batchRepository.countByStatus())
        .thenReturn(
            List.<Object[]>of(
                new Object[] {QuestionGenerationBatchStatus.PLANNED, 1L},
                new Object[] {QuestionGenerationBatchStatus.FAILED, 1L}));
    when(requestRepository.countByStatus())
        .thenReturn(
            List.<Object[]>of(new Object[] {QuestionGenerationBatchRequestStatus.PENDING, 1L}));

    QuestionGenerationBatchAdminStatusDTO status =
        statusServiceWithTaskHolders(List.of()).getStatus();

    assertThat(status.getBatchCountsByStatus().get("PLANNED"), equalTo(1L));
    assertThat(status.getBatchCountsByStatus().get("FAILED"), equalTo(1L));
    assertThat(status.getRequestCountsByStatus().get("PENDING"), equalTo(1L));
  }

  @Test
  void reportsSchedulerActiveBasedOnRegisteredMaintenanceTasks() {
    assertThat(
        statusServiceWithTaskHolders(List.of()).getStatus().isSchedulerActive(), equalTo(false));

    ScheduledTask scheduledTask = mock(ScheduledTask.class);
    when(scheduledTask.toString())
        .thenReturn(
            QuestionGenerationBatchMaintenanceJob.class.getName() + " runHourlyMaintenance");
    ScheduledTaskHolder taskHolder = () -> Set.of(scheduledTask);

    assertThat(
        statusServiceWithTaskHolders(List.of(taskHolder)).getStatus().isSchedulerActive(),
        equalTo(true));
  }

  @Test
  void reportsProdProfileAndLatestScheduledAndManualMaintenanceRunsSeparately() {
    Timestamp scheduledStartedAt = Timestamp.valueOf("2026-06-18 05:00:00");
    Timestamp scheduledFinishedAt = Timestamp.valueOf("2026-06-18 05:01:00");
    Timestamp manualStartedAt = Timestamp.valueOf("2026-06-18 06:00:00");
    Timestamp manualFinishedAt = Timestamp.valueOf("2026-06-18 06:02:00");
    environment.setActiveProfiles("prod");
    when(maintenanceRunService.findLatestRun(
            QuestionGenerationBatchMaintenanceTriggerSource.SCHEDULED))
        .thenReturn(
            Optional.of(
                maintenanceRun(
                    QuestionGenerationBatchMaintenanceTriggerSource.SCHEDULED,
                    scheduledStartedAt,
                    scheduledFinishedAt,
                    "poll failed")));
    when(maintenanceRunService.findLatestRun(
            QuestionGenerationBatchMaintenanceTriggerSource.MANUAL_RESUME))
        .thenReturn(
            Optional.of(
                maintenanceRun(
                    QuestionGenerationBatchMaintenanceTriggerSource.MANUAL_RESUME,
                    manualStartedAt,
                    manualFinishedAt,
                    null)));

    QuestionGenerationBatchAdminStatusDTO status =
        statusServiceWithTaskHolders(List.of()).getStatus();

    assertThat(status.isProdProfileActive(), equalTo(true));
    assertThat(status.getLastScheduledMaintenanceStartedAt(), equalTo(scheduledStartedAt));
    assertThat(status.getLastScheduledMaintenanceFinishedAt(), equalTo(scheduledFinishedAt));
    assertThat(status.getLastScheduledMaintenanceError(), equalTo("poll failed"));
    assertThat(status.getLastManualMaintenanceStartedAt(), equalTo(manualStartedAt));
    assertThat(status.getLastManualMaintenanceFinishedAt(), equalTo(manualFinishedAt));
    assertThat(status.getLastManualMaintenanceError(), equalTo(null));
  }

  private QuestionGenerationBatchAdminStatusService statusServiceWithTaskHolders(
      List<ScheduledTaskHolder> taskHolders) {
    return new QuestionGenerationBatchAdminStatusService(
        batchRepository,
        requestRepository,
        "configured-token",
        environment,
        taskHolders,
        maintenanceRunService);
  }

  private static QuestionGenerationBatchMaintenanceRun maintenanceRun(
      QuestionGenerationBatchMaintenanceTriggerSource triggerSource,
      Timestamp startedAt,
      Timestamp finishedAt,
      String error) {
    QuestionGenerationBatchMaintenanceRun run = new QuestionGenerationBatchMaintenanceRun();
    run.setTriggerSource(triggerSource);
    run.setStartedAt(startedAt);
    run.setFinishedAt(finishedAt);
    run.setError(error);
    return run;
  }
}
