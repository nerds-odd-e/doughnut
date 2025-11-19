package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.GlobalAiModelSettings;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GlobalSettingsControllerTest {
  GlobalSettingsController controller;
  UserModel currentUser;

  @Autowired MakeMe makeMe;
  @Autowired AuthorizationService authorizationService;

  Timestamp currentTime;

  TestabilitySettings testabilitySettings = new TestabilitySettings();
  GlobalSettingsService globalSettingsService;

  @BeforeEach
  void Setup() {
    currentTime = makeMe.aTimestamp().please();
    testabilitySettings.timeTravelTo(currentTime);
    currentUser = makeMe.anAdmin().toModelPlease();
    globalSettingsService = new GlobalSettingsService(makeMe.modelFactoryService);
    controller =
        new GlobalSettingsController(
            makeMe.modelFactoryService, currentUser, testabilitySettings, authorizationService);
  }

  @Nested
  class GetCurrentModelVersions {
    @Test
    void ShouldUseGpt35ByDefault() {
      GlobalAiModelSettings currentModelVersions = controller.getCurrentModelVersions();
      assertEquals("gpt-4o-mini", currentModelVersions.getQuestionGenerationModel());
      assertEquals("gpt-4o-mini", currentModelVersions.getEvaluationModel());
      assertEquals("gpt-4o-mini", currentModelVersions.getOthersModel());
    }

    @Test
    void ShouldUseDbSettingsIfExists() {
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
      assertEquals(
          "any-question-generation-model-version",
          currentModelVersions.getQuestionGenerationModel());
      assertEquals("any-evaluation-model-version", currentModelVersions.getEvaluationModel());
      assertEquals("any-other-model-version", currentModelVersions.getOthersModel());
    }
  }

  @Nested
  class SetCurrentModelVersions {
    GlobalAiModelSettings settings = new GlobalAiModelSettings("gpt-3.5", "gpt-4", "gpt-5");

    @Test
    void authentication() {
      controller =
          new GlobalSettingsController(
              makeMe.modelFactoryService,
              makeMe.aUser().toModelPlease(),
              testabilitySettings,
              authorizationService);
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.setCurrentModelVersions(settings));
    }

    @Test
    void setValues() throws UnexpectedNoAccessRightException {
      controller.setCurrentModelVersions(settings);
      GlobalSettingsService.GlobalSettingsKeyValue globalSettingQuestionQuestion =
          globalSettingsService.globalSettingQuestionGeneration();
      assertEquals("gpt-3.5", globalSettingQuestionQuestion.getValue());
      assertEquals(currentTime, globalSettingQuestionQuestion.getCreatedAt());
    }

    @Test
    void allValues() throws UnexpectedNoAccessRightException {
      controller.setCurrentModelVersions(settings);
      GlobalAiModelSettings currentModelVersions = controller.getCurrentModelVersions();
      assertEquals("gpt-3.5", currentModelVersions.getQuestionGenerationModel());
      assertEquals("gpt-4", currentModelVersions.getEvaluationModel());
      assertEquals("gpt-5", currentModelVersions.getOthersModel());
    }

    @Test
    void avoidDuplicate() throws UnexpectedNoAccessRightException {
      controller.setCurrentModelVersions(settings);
      long count = makeMe.modelFactoryService.globalSettingRepository.count();
      controller.setCurrentModelVersions(settings);
      assertEquals(count, makeMe.modelFactoryService.globalSettingRepository.count());
    }
  }
}
