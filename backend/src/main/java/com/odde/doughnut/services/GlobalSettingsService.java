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

  public CurrentModelVersionResponse getCurrentModelVersions() {
    String currentQuestionGenerationModelVersion =
        getGlobalSettings("current_question_generation_model_version").getValue();

    String currentEvaluationModelVersion =
        getGlobalSettings("current_evaluation_model_version").getValue();

    String currentOtherModelVersion = getGlobalSettings("current_other_model_version").getValue();

    return new CurrentModelVersionResponse(
        currentQuestionGenerationModelVersion,
        currentEvaluationModelVersion,
        currentOtherModelVersion);
  }

  public CurrentModelVersionResponse setCurrentModelVersions(
      CurrentModelVersionResponse models, Timestamp currentUTCTimestamp) {
    setKeyValue(
        currentUTCTimestamp,
        "current_question_generation_model_version",
        models.getCurrentQuestionGenerationModelVersion());
    setKeyValue(
        currentUTCTimestamp,
        "current_evaluation_model_version",
        models.getCurrentEvaluationModelVersion());
    setKeyValue(
        currentUTCTimestamp, "current_other_model_version", models.getCurrentOthersModelVersion());
    return models;
  }

  private void setKeyValue(Timestamp currentUTCTimestamp, String keyName, String value) {
    GlobalSettings currentQuestionGenerationModelVersion = getGlobalSettings(keyName);
    currentQuestionGenerationModelVersion.setValue(value);
    currentQuestionGenerationModelVersion.setCreatedAt(currentUTCTimestamp);
    modelFactoryService.globalSettingRepository.save(currentQuestionGenerationModelVersion);
  }

  private GlobalSettings getGlobalSettings(String keyName) {
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
