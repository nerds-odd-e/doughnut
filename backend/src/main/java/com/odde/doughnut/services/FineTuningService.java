package com.odde.doughnut.services;

import com.odde.doughnut.controllers.json.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.ArrayList;
import java.util.List;

public class FineTuningService {
  private final ModelFactoryService modelFactoryService;

  public FineTuningService(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  public List<SuggestedQuestionForFineTuning> getSuggestedQuestionForFineTunings() {
    List<SuggestedQuestionForFineTuning> suggestedQuestionForFineTunings = new ArrayList<>();
    modelFactoryService
        .questionSuggestionForFineTuningRepository
        .findAll()
        .forEach(suggestedQuestionForFineTunings::add);
    return suggestedQuestionForFineTunings;
  }

  public List<OpenAIChatGPTFineTuningExample> getQuestionGenerationTrainingExamples() {
    return getSuggestedQuestionForFineTunings().stream()
        .filter(SuggestedQuestionForFineTuning::isPositiveFeedback)
        .map(SuggestedQuestionForFineTuning::toQuestionGenerationFineTuningExample)
        .toList();
  }

  public List<OpenAIChatGPTFineTuningExample> getQuestionEvaluationTrainingExamples() {
    return getSuggestedQuestionForFineTunings().stream()
        .map(SuggestedQuestionForFineTuning::toQuestionEvaluationFineTuningData)
        .toList();
  }
}
