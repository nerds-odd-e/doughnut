package com.odde.doughnut.services;

import com.odde.doughnut.controllers.json.CurrentModelVersionResponse;
import com.odde.doughnut.entities.GlobalSettings;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;

public class GlobalSettingsService {

  private final ModelFactoryService modelFactoryService;

  public GlobalSettingsService(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  public GlobalSettingsModel getGlobalSettingQuestionQuestion() {
    return new GlobalSettingsModel(
        "current_question_generation_model_version", modelFactoryService);
  }

  public GlobalSettingsModel getGlobalSettingEvaluation() {
    return new GlobalSettingsModel("current_evaluation_model_version", modelFactoryService);
  }

  public GlobalSettingsModel getGlobalSettingOthers() {
    return new GlobalSettingsModel("current_other_model_version", modelFactoryService);
  }

  record GlobalSettingsModel(String keyName, ModelFactoryService modelFactoryService) {
    public String getValue() {
      return getGlobalSettings().getValue();
    }

    public void setKeyValue(Timestamp currentUTCTimestamp, String value) {
      GlobalSettings currentQuestionGenerationModelVersion = getGlobalSettings();
      currentQuestionGenerationModelVersion.setValue(value);
      currentQuestionGenerationModelVersion.setCreatedAt(currentUTCTimestamp);
      modelFactoryService.globalSettingRepository.save(currentQuestionGenerationModelVersion);
    }

    private GlobalSettings getGlobalSettings() {
      GlobalSettings currentQuestionGenerationModelVersion =
          modelFactoryService.globalSettingRepository.findByKeyName(keyName);
      if (currentQuestionGenerationModelVersion == null) {
        GlobalSettings globalSettings = new GlobalSettings();
        globalSettings.setKeyName(keyName);
        globalSettings.setValue("gpt-3.5-turbo");
        return globalSettings;
      }
      return currentQuestionGenerationModelVersion;
    }
  }

  public CurrentModelVersionResponse getCurrentModelVersions() {
    return new CurrentModelVersionResponse(
        getGlobalSettingQuestionQuestion().getValue(),
        getGlobalSettingEvaluation().getValue(),
        getGlobalSettingOthers().getValue());
  }

  public CurrentModelVersionResponse setCurrentModelVersions(
      CurrentModelVersionResponse models, Timestamp currentUTCTimestamp) {
    getGlobalSettingQuestionQuestion()
        .setKeyValue(currentUTCTimestamp, models.getCurrentQuestionGenerationModelVersion());
    getGlobalSettingEvaluation()
        .setKeyValue(currentUTCTimestamp, models.getCurrentEvaluationModelVersion());
    getGlobalSettingOthers()
        .setKeyValue(currentUTCTimestamp, models.getCurrentOthersModelVersion());
    return models;
  }
}
