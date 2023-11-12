package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.json.CurrentModelVersionResponse;
import com.odde.doughnut.entities.GlobalSettings;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestGlobalSettingsControllerTest {
  RestGlobalSettingsController controller;
  UserModel currentUser;

  @Autowired MakeMe makeMe;

  Timestamp currentTime;

  TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void Setup() {
    currentTime = makeMe.aTimestamp().please();
    testabilitySettings.timeTravelTo(currentTime);
    currentUser = makeMe.anAdmin().toModelPlease();
    controller =
        new RestGlobalSettingsController(
            makeMe.modelFactoryService, currentUser, testabilitySettings);
  }

  @Nested
  class GetCurrentModelVersions {
    @Test
    void ShouldUseGpt35ByDefault() {
      CurrentModelVersionResponse currentModelVersions = controller.getCurrentModelVersions();
      assertEquals(
          "gpt-3.5-turbo", currentModelVersions.getCurrentQuestionGenerationModelVersion());
      assertEquals("gpt-3.5-turbo", currentModelVersions.getCurrentEvaluationModelVersion());
      assertEquals("gpt-3.5-turbo", currentModelVersions.getCurrentOthersModelVersion());
    }

    @Test
    void ShouldUseDbSettingsIfExists() {
      SetUpGlobalSetting("current_evaluation_model_version", "any-evaluation-model-version");
      SetUpGlobalSetting(
          "current_question_generation_model_version", "any-question-generation-model-version");
      SetUpGlobalSetting("current_other_model_version", "any-other-model-version");
      CurrentModelVersionResponse currentModelVersions = controller.getCurrentModelVersions();
      assertEquals(
          "any-question-generation-model-version",
          currentModelVersions.getCurrentQuestionGenerationModelVersion());
      assertEquals(
          "any-evaluation-model-version", currentModelVersions.getCurrentEvaluationModelVersion());
      assertEquals("any-other-model-version", currentModelVersions.getCurrentOthersModelVersion());
    }

    private void SetUpGlobalSetting(String keyName, String value) {
      GlobalSettings globalSettings1 = new GlobalSettings();
      globalSettings1.setKeyName(keyName);
      globalSettings1.setValue(value);
      makeMe.modelFactoryService.globalSettingRepository.save(globalSettings1);
      makeMe.refresh(globalSettings1);
    }
  }

  @Nested
  class SetCurrentModelVersions {
    CurrentModelVersionResponse settings =
        new CurrentModelVersionResponse("gpt-3.5", "gpt-4", "gpt-5");

    @Test
    void authentication() {
      controller =
          new RestGlobalSettingsController(
              makeMe.modelFactoryService, makeMe.aUser().toModelPlease(), testabilitySettings);
      assertThrows(
          UnexpectedNoAccessRightException.class,
          () -> controller.setCurrentModelVersions(settings));
    }

    @Test
    void setValues() throws UnexpectedNoAccessRightException {
      controller.setCurrentModelVersions(settings);
      GlobalSettings currentQuestionGenerationModelVersion =
          makeMe.modelFactoryService.globalSettingRepository.findByKeyName(
              "current_question_generation_model_version");
      assertEquals("gpt-3.5", currentQuestionGenerationModelVersion.getValue());
      assertEquals(currentTime, currentQuestionGenerationModelVersion.getCreatedAt());
    }

    @Test
    void allValues() throws UnexpectedNoAccessRightException {
      controller.setCurrentModelVersions(settings);
      CurrentModelVersionResponse currentModelVersions = controller.getCurrentModelVersions();
      assertEquals("gpt-3.5", currentModelVersions.getCurrentQuestionGenerationModelVersion());
      assertEquals("gpt-4", currentModelVersions.getCurrentEvaluationModelVersion());
      assertEquals("gpt-5", currentModelVersions.getCurrentOthersModelVersion());
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
