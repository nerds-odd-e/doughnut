package com.odde.doughnut.services;

import java.sql.Timestamp;
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

  public QuestionGenerationBatchMaintenanceJob(
      QuestionGenerationBatchMaintenanceService maintenanceService,
      QuestionGenerationBatchSubmitDueUsersService submitDueUsersService) {
    this.maintenanceService = maintenanceService;
    this.submitDueUsersService = submitDueUsersService;
  }

  @Scheduled(cron = "0 0 * * * *")
  public void runHourlyMaintenance() {
    Timestamp currentTime = new Timestamp(System.currentTimeMillis());
    logger.info("Question generation batch hourly maintenance started at {}", currentTime);
    try {
      maintenanceService.resumeExistingBatches(currentTime);
    } catch (RuntimeException e) {
      logger.warn("Question generation batch resume step failed; continuing to due submissions", e);
    }
    try {
      submitDueUsersService.submitDueUsers(currentTime);
    } finally {
      logger.info("Question generation batch hourly maintenance finished at {}", currentTime);
    }
  }
}
