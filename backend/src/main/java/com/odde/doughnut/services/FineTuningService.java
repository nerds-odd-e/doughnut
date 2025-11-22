package com.odde.doughnut.services;

import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.entities.repositories.QuestionSuggestionForFineTuningRepository;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FineTuningService {
  private final QuestionSuggestionForFineTuningRepository questionSuggestionForFineTuningRepository;

  public final OpenAiApiHandler openAiApiHandler;

  public FineTuningService(
      QuestionSuggestionForFineTuningRepository questionSuggestionForFineTuningRepository,
      OpenAiApiHandler openAiApiHandler) {
    this.questionSuggestionForFineTuningRepository = questionSuggestionForFineTuningRepository;
    this.openAiApiHandler = openAiApiHandler;
  }

  public List<SuggestedQuestionForFineTuning> getSuggestedQuestionForFineTunings() {
    List<SuggestedQuestionForFineTuning> suggestedQuestionForFineTunings = new ArrayList<>();
    questionSuggestionForFineTuningRepository
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
