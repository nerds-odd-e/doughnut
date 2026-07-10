package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchAdminStatusDTO;
import com.odde.doughnut.controllers.dto.QuestionGenerationBatchSubmissionSummaryDTO;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.GlobalSettingsService;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AdminQuestionGenerationBatchControllerTest extends ControllerTestBase {

  @Autowired AdminQuestionGenerationBatchController controller;
  @Autowired GlobalSettingsService globalSettingsService;

  Timestamp currentTime;

  @BeforeEach
  void setup() {
    currentTime = makeMe.aTimestamp().please();
    globalSettingsService
        .globalSettingQuestionGeneration()
        .setKeyValue(currentTime, "gpt-batch-question-generation");
  }

  @Test
  void adminGetsZeroFilledStatusMapsWhenNoBatchesExist() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());

    QuestionGenerationBatchAdminStatusDTO status = controller.getQuestionGenerationBatchStatus();

    for (QuestionGenerationBatchStatus batchStatus : QuestionGenerationBatchStatus.values()) {
      assertThat(status.getBatchCountsByStatus().get(batchStatus.name()), equalTo(0L));
    }
    for (QuestionGenerationBatchRequestStatus requestStatus :
        QuestionGenerationBatchRequestStatus.values()) {
      assertThat(status.getRequestCountsByStatus().get(requestStatus.name()), equalTo(0L));
    }
    assertThat(status.isOpenAiTokenConfigured(), equalTo(true));
    assertThat(status.isSchedulerActive(), equalTo(false));
  }

  @Test
  void nonAdminCannotGetStatus() {
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.getQuestionGenerationBatchStatus());
  }

  @Test
  void adminCanTriggerRecentRecallUsersSubmission() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());

    QuestionGenerationBatchSubmissionSummaryDTO summary = controller.submitRecentRecallUsers();

    assertThat(summary.getConsideredUserCount(), equalTo(0));
    assertThat(summary.getSubmittedCount(), equalTo(0));
    assertThat(summary.getFailedCount(), equalTo(0));
    assertThat(summary.getSkippedCount(), equalTo(0));
  }

  @Test
  void nonAdminCannotTriggerRecentRecallUsersSubmission() {
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.submitRecentRecallUsers());
  }

  @Test
  void adminGetsNullMaintenanceRunTimestampsWhenNoRunsExist()
      throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());

    QuestionGenerationBatchAdminStatusDTO status = controller.getQuestionGenerationBatchStatus();

    assertThat(status.getLastScheduledMaintenanceStartedAt(), equalTo(null));
    assertThat(status.getLastManualMaintenanceStartedAt(), equalTo(null));
  }

  @Test
  void adminCanResumeExistingBatchesAndReceivesRefreshedStatus()
      throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());
    testabilitySettings.timeTravelTo(currentTime);

    QuestionGenerationBatchAdminStatusDTO status = controller.resumeExistingBatches();

    assertThat(status.getBatchCountsByStatus().get("SUBMITTED"), equalTo(0L));
    assertThat(status.getRequestCountsByStatus().get("PENDING"), equalTo(0L));
    assertThat(status.getLastManualMaintenanceStartedAt(), equalTo(currentTime));
    assertThat(status.getLastManualMaintenanceFinishedAt(), notNullValue());
  }

  @Test
  void nonAdminCannotResumeExistingBatches() {
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.resumeExistingBatches());
  }
}
