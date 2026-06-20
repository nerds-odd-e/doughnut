package com.odde.doughnut.services;

import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
import java.sql.Timestamp;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class QuestionGenerationBatchMaintenanceJob {
  private static final Logger logger =
      LoggerFactory.getLogger(QuestionGenerationBatchMaintenanceJob.class);

  private final QuestionGenerationBatchMaintenanceService maintenanceService;
  private final QuestionGenerationBatchSubmitDueUsersService submitDueUsersService;
  private final QuestionGenerationBatchMaintenanceRunService maintenanceRunService;

  public QuestionGenerationBatchMaintenanceJob(
      QuestionGenerationBatchMaintenanceService maintenanceService,
      QuestionGenerationBatchSubmitDueUsersService submitDueUsersService,
      QuestionGenerationBatchMaintenanceRunService maintenanceRunService) {
    this.maintenanceService = maintenanceService;
    this.submitDueUsersService = submitDueUsersService;
    this.maintenanceRunService = maintenanceRunService;
  }

  @Scheduled(cron = "0 0 * * * *")
  @SchedulerLock(
      name = "questionGenerationBatchHourlyMaintenance",
      lockAtMostFor = "55m",
      lockAtLeastFor = "1m")
  public void runHourlyMaintenance() {
    Timestamp currentTime = new Timestamp(System.currentTimeMillis());
    maintenanceRunService.recordStarted(
        QuestionGenerationBatchMaintenanceTriggerSource.SCHEDULED, currentTime);
    logger.info("Question generation batch hourly maintenance started at {}", currentTime);
    try {
      maintenanceService.resumeExistingBatches(currentTime);
    } catch (RuntimeException e) {
      maintenanceRunService.recordError(e);
      logger.warn("Question generation batch resume step failed; continuing to due submissions", e);
    }
    try {
      submitDueUsersService.submitDueUsers(currentTime);
    } catch (RuntimeException e) {
      maintenanceRunService.recordError(e);
      throw e;
    } finally {
      maintenanceRunService.recordFinished(new Timestamp(System.currentTimeMillis()));
      logger.info("Question generation batch hourly maintenance finished at {}", currentTime);
    }
  }
}
