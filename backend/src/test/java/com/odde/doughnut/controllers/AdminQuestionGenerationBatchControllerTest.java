package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class AdminQuestionGenerationBatchControllerTest extends ControllerTestBase {

  @Autowired AdminQuestionGenerationBatchController controller;
  @Autowired GlobalSettingsService globalSettingsService;

  Timestamp currentTime;

  @BeforeEach
  void setup() {
    currentTime = makeMe.aTimestamp().please();
    testabilitySettings.timeTravelTo(currentTime);
    globalSettingsService
        .globalSettingQuestionGeneration()
        .setKeyValue(currentTime, "gpt-batch-question-generation");
  }

  @Test
  void adminGetsZeroFilledStatusWhenNoBatchesExist() throws UnexpectedNoAccessRightException {
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
    assertThat(status.getLastScheduledMaintenanceStartedAt(), nullValue());
    assertThat(status.getLastManualMaintenanceStartedAt(), nullValue());
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
  void adminResumeRecordsManualMaintenanceTimestamps() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());

    QuestionGenerationBatchAdminStatusDTO status = controller.resumeExistingBatches();

    assertThat(status.getLastManualMaintenanceStartedAt(), equalTo(currentTime));
    assertThat(status.getLastManualMaintenanceFinishedAt(), notNullValue());
    assertThat(status.getLastManualMaintenanceError(), nullValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {"status", "submit", "resume"})
  void nonAdminDenied(String action) {
    currentUser.setUser(makeMe.aUser().please());
    assertThrows(UnexpectedNoAccessRightException.class, actionFor(action));
  }

  private Executable actionFor(String action) {
    return switch (action) {
      case "status" -> controller::getQuestionGenerationBatchStatus;
      case "submit" -> controller::submitRecentRecallUsers;
      case "resume" -> controller::resumeExistingBatches;
      default -> throw new IllegalArgumentException(action);
    };
  }
}
