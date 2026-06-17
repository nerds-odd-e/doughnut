package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchAdminStatusDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.QuestionGenerationBatchPlanningService;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AdminQuestionGenerationBatchControllerTest extends ControllerTestBase {

  @Autowired AdminQuestionGenerationBatchController controller;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired GlobalSettingsService globalSettingsService;

  User user;
  Timestamp currentTime;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
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
  void adminGetsCountsReflectingSeededBatchesAndRequests() throws UnexpectedNoAccessRightException {
    currentUser.setUser(makeMe.anAdmin().please());
    seedPlannedBatchWithPendingRequest();
    seedFailedBatch();

    QuestionGenerationBatchAdminStatusDTO status = controller.getQuestionGenerationBatchStatus();

    assertThat(status.getBatchCountsByStatus().get("PLANNED"), equalTo(1L));
    assertThat(status.getBatchCountsByStatus().get("FAILED"), equalTo(1L));
    assertThat(status.getRequestCountsByStatus().get("PENDING"), equalTo(1L));
  }

  @Test
  void nonAdminCannotGetStatus() {
    currentUser.setUser(makeMe.aUser().please());

    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.getQuestionGenerationBatchStatus());
  }

  private void seedPlannedBatchWithPendingRequest() {
    givenMemoryTrackerDueLater();
    planningService.planLocalBatchForUser(user, currentTime).orElseThrow();
  }

  private void seedFailedBatch() {
    QuestionGenerationBatch batch = new QuestionGenerationBatch();
    batch.setUser(user);
    batch.setStatus(QuestionGenerationBatchStatus.FAILED);
    batch.setPlannedAt(currentTime);
    batchRepository.save(batch);
  }

  private void givenMemoryTrackerDueLater() {
    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    makeMe
        .aMemoryTrackerFor(note)
        .by(user)
        .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
        .please();
  }
}
