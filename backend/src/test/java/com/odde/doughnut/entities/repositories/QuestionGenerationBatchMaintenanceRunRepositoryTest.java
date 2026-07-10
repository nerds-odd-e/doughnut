package com.odde.doughnut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
import com.odde.doughnut.services.QuestionGenerationBatchMaintenanceJob;
import com.odde.doughnut.services.QuestionGenerationBatchMaintenanceRunService;
import com.odde.doughnut.services.QuestionGenerationBatchMaintenanceService;
import com.odde.doughnut.services.QuestionGenerationBatchSubmitDueUsersService;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchMaintenanceRunRepositoryTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchMaintenanceRunRepository repository;
  @Autowired QuestionGenerationBatchMaintenanceRunService maintenanceRunService;
  @Autowired QuestionGenerationBatchMaintenanceService maintenanceService;
  @Autowired QuestionGenerationBatchSubmitDueUsersService submitDueUsersService;

  @Test
  void persistsManualResumeMaintenanceRunWhenRecorded() {
    Timestamp startedAt = makeMe.aTimestamp().please();
    Timestamp finishedAt = makeMe.aTimestamp().please();

    maintenanceRunService.recordStarted(
        QuestionGenerationBatchMaintenanceTriggerSource.MANUAL_RESUME, startedAt);
    maintenanceRunService.recordFinished(finishedAt);

    var latestRun =
        repository
            .findTopByTriggerSourceOrderByStartedAtDesc(
                QuestionGenerationBatchMaintenanceTriggerSource.MANUAL_RESUME)
            .orElseThrow();
    assertThat(
        latestRun.getTriggerSource(),
        equalTo(QuestionGenerationBatchMaintenanceTriggerSource.MANUAL_RESUME));
    assertThat(latestRun.getStartedAt(), equalTo(startedAt));
    assertThat(latestRun.getFinishedAt(), equalTo(finishedAt));
    assertThat(latestRun.getError(), equalTo(null));
  }

  @Test
  void persistsScheduledMaintenanceRunWhenHourlyJobRuns() {
    QuestionGenerationBatchMaintenanceJob job =
        new QuestionGenerationBatchMaintenanceJob(
            maintenanceService, submitDueUsersService, maintenanceRunService);

    job.runHourlyMaintenance();

    var latestRun =
        repository
            .findTopByTriggerSourceOrderByStartedAtDesc(
                QuestionGenerationBatchMaintenanceTriggerSource.SCHEDULED)
            .orElseThrow();
    assertThat(
        latestRun.getTriggerSource(),
        equalTo(QuestionGenerationBatchMaintenanceTriggerSource.SCHEDULED));
    assertThat(latestRun.getStartedAt(), notNullValue());
    assertThat(latestRun.getFinishedAt(), notNullValue());
    assertThat(latestRun.getError(), equalTo(null));
  }
}
