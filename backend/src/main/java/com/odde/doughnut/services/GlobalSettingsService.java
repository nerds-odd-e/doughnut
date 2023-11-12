package com.odde.doughnut.services;

import com.odde.doughnut.controllers.json.CurrentModelVersionResponse;
import com.odde.doughnut.entities.GlobalSettings;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class GlobalSettingsService {

  private final ModelFactoryService modelFactoryService;

  public GlobalSettingsService(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  public CurrentModelVersionResponse getCurrentModelVersions() {
    Iterable<GlobalSettings> all = modelFactoryService.globalSettingRepository.findAll();
    List<GlobalSettings> globalSettings = new ArrayList<>();
    all.forEach(globalSettings::add);

    String currentQuestionGenerationModelVersion =
        getModelVersion(globalSettings, "current_question_generation_model_version");

    String currentEvaluationModelVersion =
        getModelVersion(globalSettings, "current_evaluation_model_version");

    String currentOtherModelVersion =
        getModelVersion(globalSettings, "current_other_model_version");

    return new CurrentModelVersionResponse(
        currentQuestionGenerationModelVersion,
        currentEvaluationModelVersion,
        currentOtherModelVersion);
  }

  private static String getModelVersion(List<GlobalSettings> globalSettings, String keyName) {
    return globalSettings.stream()
        .filter(g -> g.getKeyName().equals(keyName))
        .findFirst()
        .map(GlobalSettings::getValue)
        .orElse("gpt-3.5-turbo");
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

  private void setKeyValue(
      Timestamp currentUTCTimestamp,
      String currentQuestionGenerationModelVersion1,
      String currentQuestionGenerationModelVersion2) {
    GlobalSettings currentQuestionGenerationModelVersion = new GlobalSettings();
    currentQuestionGenerationModelVersion.setKeyName(currentQuestionGenerationModelVersion1);
    currentQuestionGenerationModelVersion.setValue(currentQuestionGenerationModelVersion2);
    currentQuestionGenerationModelVersion.setCreatedAt(currentUTCTimestamp);
    modelFactoryService.globalSettingRepository.save(currentQuestionGenerationModelVersion);
  }
}
