package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchAdminStatusDTO;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.List;
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
  QuestionGenerationBatchMaintenanceRunState maintenanceRunState;

  @BeforeEach
  void setup() {
    batchRepository = mock(QuestionGenerationBatchRepository.class);
    requestRepository = mock(QuestionGenerationBatchRequestRepository.class);
    environment = mock(Environment.class);
    maintenanceRunState = new QuestionGenerationBatchMaintenanceRunState();
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
            maintenanceRunState);
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
  void reportsProdProfileAndLastMaintenanceStateSeparately() {
    Timestamp startedAt = Timestamp.valueOf("2026-06-18 05:00:00");
    Timestamp finishedAt = Timestamp.valueOf("2026-06-18 05:01:00");
    when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
    maintenanceRunState.recordStarted(startedAt);
    maintenanceRunState.recordError(new RuntimeException("poll failed"));
    maintenanceRunState.recordFinished(finishedAt);

    QuestionGenerationBatchAdminStatusDTO status =
        statusServiceWithTaskHolders(List.of()).getStatus();

    assertThat(status.isProdProfileActive(), equalTo(true));
    assertThat(status.getLastMaintenanceStartedAt(), equalTo(startedAt));
    assertThat(status.getLastMaintenanceFinishedAt(), equalTo(finishedAt));
    assertThat(status.getLastMaintenanceError(), equalTo("poll failed"));
  }

  private QuestionGenerationBatchAdminStatusService statusServiceWithTaskHolders(
      List<ScheduledTaskHolder> taskHolders) {
    return new QuestionGenerationBatchAdminStatusService(
        batchRepository,
        requestRepository,
        "configured-token",
        environment,
        taskHolders,
        maintenanceRunState);
  }

  private ScheduledTask scheduledTaskFor(QuestionGenerationBatchMaintenanceJob job)
      throws Exception {
    Method method = QuestionGenerationBatchMaintenanceJob.class.getMethod("runHourlyMaintenance");
    Task task = new Task(new ScheduledMethodRunnable(job, method));
    Constructor<ScheduledTask> constructor = ScheduledTask.class.getDeclaredConstructor(Task.class);
    constructor.setAccessible(true);
    return constructor.newInstance(task);
  }
}
