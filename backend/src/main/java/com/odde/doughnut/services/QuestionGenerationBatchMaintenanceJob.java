package com.odde.doughnut.services;

import java.sql.Timestamp;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class QuestionGenerationBatchMaintenanceJob {
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
    maintenanceService.resumeExistingBatches(currentTime);
    submitDueUsersService.submitDueUsers(currentTime);
  }
}
