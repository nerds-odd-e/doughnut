package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.donut.controllers.dto.QuestionGenerationBatchAdminStatusDTO;
import com.odde.donut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
import com.odde.donut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.donut.entities.QuestionGenerationBatchStatus;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.donut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchAdminStatusServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository requestRepository;
  @Autowired QuestionGenerationBatchMaintenanceRunService maintenanceRunService;

  StandardEnvironment environment;

  @BeforeEach
  void setup() {
    environment = new StandardEnvironment();
  }

  @Test
  void reportsBatchAndRequestCountsFromRepositories() {
    User user = makeMe.aUser().please();
    Timestamp now = makeMe.aTimestamp().please();
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .status(QuestionGenerationBatchStatus.PLANNED)
        .plannedAt(now)
        .please();
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .status(QuestionGenerationBatchStatus.FAILED)
        .plannedAt(now)
        .please();
    var batch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(user)
            .status(QuestionGenerationBatchStatus.COMPLETED)
            .plannedAt(now)
            .please();
    makeMe.entityPersister.flush();
    var note = makeMe.aNote().notebookOwnedBy(user).please();
    var tracker = makeMe.aMemoryTrackerFor(note).please();
    makeMe
        .aQuestionGenerationBatchRequest()
        .batch(batch)
        .memoryTracker(tracker)
        .status(QuestionGenerationBatchRequestStatus.PENDING)
        .please();

    QuestionGenerationBatchAdminStatusDTO status =
        statusServiceWithTaskHolders(List.of()).getStatus();

    assertThat(status.getBatchCountsByStatus().get("PLANNED"), equalTo(1L));
    assertThat(status.getBatchCountsByStatus().get("FAILED"), equalTo(1L));
    assertThat(status.getRequestCountsByStatus().get("PENDING"), equalTo(1L));
  }

  @Test
  void reportsSchedulerInactiveWithoutRegisteredMaintenanceTasks() {
    assertThat(
        statusServiceWithTaskHolders(List.of()).getStatus().isSchedulerActive(), equalTo(false));
  }

  @Test
  void reportsSchedulerActiveWhenMaintenanceTaskIsRegistered() {
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
  void reportsProdProfileActiveWhenProdProfileSet() {
    environment.setActiveProfiles("prod");

    assertThat(
        statusServiceWithTaskHolders(List.of()).getStatus().isProdProfileActive(), equalTo(true));
  }

  @Test
  void reportsLatestScheduledAndManualMaintenanceRunsSeparately() {
    Timestamp scheduledStartedAt = Timestamp.valueOf("2026-06-18 05:00:00");
    Timestamp scheduledFinishedAt = Timestamp.valueOf("2026-06-18 05:01:00");
    Timestamp manualStartedAt = Timestamp.valueOf("2026-06-18 06:00:00");
    Timestamp manualFinishedAt = Timestamp.valueOf("2026-06-18 06:02:00");

    maintenanceRunService.recordStarted(
        QuestionGenerationBatchMaintenanceTriggerSource.SCHEDULED, scheduledStartedAt);
    maintenanceRunService.recordError(new RuntimeException("poll failed"));
    maintenanceRunService.recordFinished(scheduledFinishedAt);

    maintenanceRunService.recordStarted(
        QuestionGenerationBatchMaintenanceTriggerSource.MANUAL_RESUME, manualStartedAt);
    maintenanceRunService.recordFinished(manualFinishedAt);

    QuestionGenerationBatchAdminStatusDTO status =
        statusServiceWithTaskHolders(List.of()).getStatus();

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
}
