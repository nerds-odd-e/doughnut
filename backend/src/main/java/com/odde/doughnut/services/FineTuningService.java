package com.odde.doughnut.services;

import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.fine_tuning.FineTuningJobRequest;
import com.theokanning.openai.fine_tuning.Hyperparameters;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;

public class FineTuningService {
  private final ModelFactoryService modelFactoryService;

  private final OpenAiApiHandler openAiApiHandler;

  public FineTuningService(ModelFactoryService modelFactoryService, OpenAiApi openAiApi) {
    this.modelFactoryService = modelFactoryService;
    this.openAiApiHandler = new OpenAiApiHandler(openAiApi);
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

  public void triggerFineTune(String fileId) {
    FineTuningJobRequest fineTuningJobRequest = new FineTuningJobRequest();
    fineTuningJobRequest.setTrainingFile(fileId);
    fineTuningJobRequest.setModel("gpt-3.5-turbo-1106");
    fineTuningJobRequest.setHyperparameters(
        new Hyperparameters(3)); // not sure what should be the nEpochs value

    this.openAiApiHandler.triggerFineTune(fineTuningJobRequest);
  }

  public void uploadDataAndGTriggerFineTuning() throws IOException {
    String question =
        openAiApiHandler.uploadFineTuningExamples(
            getQuestionGenerationTrainingExamples(), "Question");
    String evaluation =
        openAiApiHandler.uploadFineTuningExamples(
            getQuestionEvaluationTrainingExamples(), "Evaluation");

    try {
      triggerFineTune(question);
      triggerFineTune(evaluation);
    } catch (Exception e) {
      throw new OpenAIServiceErrorException("Training failed.", HttpStatus.BAD_REQUEST);
    }
  }
}
