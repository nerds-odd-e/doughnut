package com.odde.doughnut.entities.repositories;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
import com.odde.doughnut.services.QuestionGenerationBatchMaintenanceJob;
import com.odde.doughnut.services.QuestionGenerationBatchMaintenanceRunService;
import com.odde.doughnut.services.QuestionGenerationBatchMaintenanceService;
import com.odde.doughnut.services.QuestionGenerationBatchSubmitDueUsersService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchMaintenanceRunRepositoryTest {

  @Autowired QuestionGenerationBatchMaintenanceRunRepository repository;
  @Autowired QuestionGenerationBatchMaintenanceRunService maintenanceRunService;
  @Autowired QuestionGenerationBatchMaintenanceService maintenanceService;
  @Autowired QuestionGenerationBatchSubmitDueUsersService submitDueUsersService;

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
