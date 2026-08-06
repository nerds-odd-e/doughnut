package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.GlobalAiModelSettings;
import com.odde.doughnut.entities.repositories.GlobalSettingRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.GlobalSettingsService;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GlobalSettingsControllerTest extends ControllerTestBase {
  @Autowired GlobalSettingsController controller;
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired GlobalSettingRepository globalSettingRepository;

  Timestamp currentTime;

  @BeforeEach
  void setup() {
    currentTime = makeMe.aTimestamp().please();
    testabilitySettings.timeTravelTo(currentTime);
    currentUser.setUser(makeMe.anAdmin().please());
  }

  @Nested
  class GetCurrentModelVersions {
    @Test
    void defaultsToConfiguredChatModel() {
      GlobalAiModelSettings currentModelVersions = controller.getCurrentModelVersions();
      assertThat(
          currentModelVersions.getQuestionGenerationModel(),
          equalTo(GlobalSettingsService.DEFAULT_CHAT_MODEL));
      assertThat(
          currentModelVersions.getEvaluationModel(),
          equalTo(GlobalSettingsService.DEFAULT_CHAT_MODEL));
      assertThat(
          currentModelVersions.getOthersModel(), equalTo(GlobalSettingsService.DEFAULT_CHAT_MODEL));
    }

    @Test
    void usesPersistedSettingsWhenPresent() {
      globalSettingsService
          .globalSettingEvaluation()
          .setKeyValue(currentTime, "any-evaluation-model-version");
      globalSettingsService
          .globalSettingQuestionGeneration()
          .setKeyValue(currentTime, "any-question-generation-model-version");
      globalSettingsService
          .globalSettingOthers()
          .setKeyValue(currentTime, "any-other-model-version");

      GlobalAiModelSettings currentModelVersions = controller.getCurrentModelVersions();

      assertThat(
          currentModelVersions.getQuestionGenerationModel(),
          equalTo("any-question-generation-model-version"));
      assertThat(
          currentModelVersions.getEvaluationModel(), equalTo("any-evaluation-model-version"));
      assertThat(currentModelVersions.getOthersModel(), equalTo("any-other-model-version"));
    }
  }

  @Nested
  class SetCurrentModelVersions {
    GlobalAiModelSettings settings = new GlobalAiModelSettings("gpt-3.5", "gpt-4", "gpt-5");

    @Test
    void nonAdminDenied() {
      currentUser.setUser(makeMe.aUser().please());
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.setCurrentModelVersions(settings));
    }

    @Test
    void persistsAllModelSettings() throws UnexpectedNoAccessRightException {
      controller.setCurrentModelVersions(settings);

      GlobalAiModelSettings currentModelVersions = controller.getCurrentModelVersions();
      assertThat(currentModelVersions.getQuestionGenerationModel(), equalTo("gpt-3.5"));
      assertThat(currentModelVersions.getEvaluationModel(), equalTo("gpt-4"));
      assertThat(currentModelVersions.getOthersModel(), equalTo("gpt-5"));
      assertThat(
          globalSettingsService.globalSettingQuestionGeneration().getCreatedAt(),
          equalTo(currentTime));
    }

    @Test
    void avoidsDuplicateRowsForSameValues() throws UnexpectedNoAccessRightException {
      controller.setCurrentModelVersions(settings);
      long count = globalSettingRepository.count();

      controller.setCurrentModelVersions(settings);

      assertThat(globalSettingRepository.count(), equalTo(count));
    }
  }
}
