package com.odde.doughnut.services;

import com.odde.doughnut.controllers.json.CurrentModelVersionResponse;
import com.odde.doughnut.entities.GlobalSettings;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.ArrayList;
import java.util.List;

public class AiModelService {

  private final ModelFactoryService modelFactoryService;

  public AiModelService(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  public CurrentModelVersionResponse getCurrentModelVersions() {
    Iterable<GlobalSettings> all = modelFactoryService.globalSettingRepository.findAll();
    List<GlobalSettings> globalSettings = new ArrayList<>();
    all.forEach(globalSettings::add);

    String currentQuestionGenerationModelVersion =
        globalSettings.stream()
            .filter(g -> g.getKey().equals("current_question_generation_model_version"))
            .findFirst()
            .map(GlobalSettings::getValue)
            .orElse("gpt-3.5-turbol");

    return new CurrentModelVersionResponse(currentQuestionGenerationModelVersion, "gpt-3.5", null);
  }
}
