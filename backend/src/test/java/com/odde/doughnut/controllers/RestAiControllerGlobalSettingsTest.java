package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.odde.doughnut.controllers.json.CurrentModelVersionResponse;
import com.odde.doughnut.entities.GlobalSettings;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestAiControllerGlobalSettingsTest {
  RestAiController controller;
  UserModel currentUser;

  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;

  @BeforeEach
  void Setup() {
    currentUser = makeMe.aUser().toModelPlease();
    controller = new RestAiController(openAiApi, makeMe.modelFactoryService, currentUser);
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
    @Test
    void setValues() {
      CurrentModelVersionResponse settings =
          new CurrentModelVersionResponse("gpt-3.5", "gpt-4", "gpt-5");
      controller.setCurrentModelVersions(settings);
      CurrentModelVersionResponse currentModelVersions = controller.getCurrentModelVersions();
      assertEquals("gpt-3.5", currentModelVersions.getCurrentQuestionGenerationModelVersion());
      //      assertEquals("gpt-4", currentModelVersions.getCurrentEvaluationModelVersion());
      //      assertEquals("gpt-5", currentModelVersions.getCurrentOthersModelVersion());
    }
  }
}
