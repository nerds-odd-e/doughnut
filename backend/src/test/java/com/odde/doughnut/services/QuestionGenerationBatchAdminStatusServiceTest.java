package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchAdminStatusDTO;
import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceRun;
import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.support.ScheduledMethodRunnable;

class QuestionGenerationBatchAdminStatusServiceTest {

  QuestionGenerationBatchRepository batchRepository;
  QuestionGenerationBatchRequestRepository requestRepository;
  Environment environment;
  QuestionGenerationBatchMaintenanceRunService maintenanceRunService;

  @BeforeEach
  void setup() {
    batchRepository = mock(QuestionGenerationBatchRepository.class);
    requestRepository = mock(QuestionGenerationBatchRequestRepository.class);
    environment = mock(Environment.class);
    maintenanceRunService = mock(QuestionGenerationBatchMaintenanceRunService.class);
    when(batchRepository.countByStatus()).thenReturn(List.of());
    when(requestRepository.countByStatus()).thenReturn(List.of());
    when(environment.getActiveProfiles()).thenReturn(new String[] {});
  }

  @Test
  void reportsRegisteredMaintenanceSchedulerFromScheduledTaskRegistry() throws Exception {
    QuestionGenerationBatchMaintenanceJob job =
        new QuestionGenerationBatchMaintenanceJob(
            mock(QuestionGenerationBatchMaintenanceService.class),
            mock(QuestionGenerationBatchSubmitDueUsersService.class),
            maintenanceRunService);
    ScheduledTask scheduledTask = scheduledTaskFor(job);
    ScheduledTaskHolder taskHolder = () -> Set.of(scheduledTask);

    QuestionGenerationBatchAdminStatusDTO status =
        statusServiceWithTaskHolders(List.of(taskHolder)).getStatus();

    assertThat(status.isSchedulerActive(), equalTo(true));
  }

  @Test
  void doesNotReportSchedulerActiveWhenNoMaintenanceTaskIsRegistered() {
    QuestionGenerationBatchAdminStatusDTO status =
        statusServiceWithTaskHolders(List.of()).getStatus();

    assertThat(status.isSchedulerActive(), equalTo(false));
  }

  @Test
  void reportsProdProfileAndLatestScheduledAndManualMaintenanceRunsSeparately() {
    Timestamp scheduledStartedAt = Timestamp.valueOf("2026-06-18 05:00:00");
    Timestamp scheduledFinishedAt = Timestamp.valueOf("2026-06-18 05:01:00");
    Timestamp manualStartedAt = Timestamp.valueOf("2026-06-18 06:00:00");
    Timestamp manualFinishedAt = Timestamp.valueOf("2026-06-18 06:02:00");
    when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
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

  private ScheduledTask scheduledTaskFor(QuestionGenerationBatchMaintenanceJob job)
      throws Exception {
    Method method = QuestionGenerationBatchMaintenanceJob.class.getMethod("runHourlyMaintenance");
    Task task = new Task(new ScheduledMethodRunnable(job, method));
    Constructor<ScheduledTask> constructor = ScheduledTask.class.getDeclaredConstructor(Task.class);
    constructor.setAccessible(true);
    return constructor.newInstance(task);
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
