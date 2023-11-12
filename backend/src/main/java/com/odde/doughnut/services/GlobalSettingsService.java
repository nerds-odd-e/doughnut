package com.odde.doughnut.services;

import com.odde.doughnut.controllers.json.GlobalAiModelSettings;
import com.odde.doughnut.entities.GlobalSettings;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;

public class GlobalSettingsService {

  private final ModelFactoryService modelFactoryService;

  public GlobalSettingsService(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  public GlobalSettingsModel getGlobalSettingQuestionQuestion() {
    return new GlobalSettingsModel("question_generation_model", modelFactoryService);
  }

  public GlobalSettingsModel getGlobalSettingEvaluation() {
    return new GlobalSettingsModel("evaluation_model", modelFactoryService);
  }

  public GlobalSettingsModel getGlobalSettingOthers() {
    return new GlobalSettingsModel("others_model", modelFactoryService);
  }

  public record GlobalSettingsModel(String keyName, ModelFactoryService modelFactoryService) {
    public String getValue() {
      return getGlobalSettings().getValue();
    }

    public void setKeyValue(Timestamp currentUTCTimestamp, String value) {
      GlobalSettings settings = getGlobalSettings();
      settings.setValue(value);
      settings.setCreatedAt(currentUTCTimestamp);
      modelFactoryService.globalSettingRepository.save(settings);
    }

    public Timestamp getCreatedAt() {
      return getGlobalSettings().getCreatedAt();
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

  public GlobalAiModelSettings getCurrentModelVersions() {
    return new GlobalAiModelSettings(
        getGlobalSettingQuestionQuestion().getValue(),
        getGlobalSettingEvaluation().getValue(),
        getGlobalSettingOthers().getValue());
  }

  public GlobalAiModelSettings setCurrentModelVersions(
      GlobalAiModelSettings models, Timestamp currentUTCTimestamp) {
    getGlobalSettingQuestionQuestion()
        .setKeyValue(currentUTCTimestamp, models.getQuestionGenerationModel());
    getGlobalSettingEvaluation().setKeyValue(currentUTCTimestamp, models.getEvaluationModel());
    getGlobalSettingOthers().setKeyValue(currentUTCTimestamp, models.getOthersModel());
    return models;
  }
}
