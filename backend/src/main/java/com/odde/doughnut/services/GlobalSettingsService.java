package com.odde.doughnut.services;

import com.odde.doughnut.controllers.json.CurrentModelVersionResponse;
import com.odde.doughnut.entities.GlobalSettings;
import com.odde.doughnut.factoryServices.ModelFactoryService;
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

  public CurrentModelVersionResponse setCurrentModelVersions(CurrentModelVersionResponse models) {
    GlobalSettings currentQuestionGenerationModelVersion = new GlobalSettings();
    currentQuestionGenerationModelVersion.setKeyName("current_question_generation_model_version");
    currentQuestionGenerationModelVersion.setValue(
        models.getCurrentQuestionGenerationModelVersion());
    modelFactoryService.globalSettingRepository.save(currentQuestionGenerationModelVersion);
    return models;
  }
}
