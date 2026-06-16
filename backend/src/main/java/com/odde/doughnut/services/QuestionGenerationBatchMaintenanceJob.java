package com.odde.doughnut.services;

import java.sql.Timestamp;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class QuestionGenerationBatchMaintenanceJob {
  private final QuestionGenerationBatchMaintenanceService maintenanceService;
  private final QuestionGenerationBatchSubmissionService submissionService;

  public QuestionGenerationBatchMaintenanceJob(
      QuestionGenerationBatchMaintenanceService maintenanceService,
      QuestionGenerationBatchSubmissionService submissionService) {
    this.maintenanceService = maintenanceService;
    this.submissionService = submissionService;
  }

  @Scheduled(cron = "0 0 * * * *")
  public void runHourlyMaintenance() {
    Timestamp currentTime = new Timestamp(System.currentTimeMillis());
    maintenanceService.resumeExistingBatches(currentTime);
    submissionService.submitDueUsers(currentTime);
  }
}
