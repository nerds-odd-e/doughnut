package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.json.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.controllers.json.UploadFineTuningExamplesResponse;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

  public UploadFineTuningExamplesResponse getUploadFineTuningExamplesResponse() throws IOException {
    var result = new UploadFineTuningExamplesResponse();
    var feedbacks = getQuestionGenerationTrainingExamples();
    if (feedbacks.size() < 10) {
      result.setMessage("Positive feedback cannot be less than 10.");
      result.setSuccess(false);
      return result;
    }
    String jsonString = getJsonString(feedbacks);
    var fileName = String.format("Question-%s.jsonl", System.currentTimeMillis());
    Path file = Path.of(fileName);
    Files.createFile(file);
    Files.write(file, jsonString.getBytes(), StandardOpenOption.WRITE);
    result.setSuccess(true);
    return result;
  }

  private String getJsonString(List<OpenAIChatGPTFineTuningExample> feedbacks)
      throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    String jsonString = "";
    for (OpenAIChatGPTFineTuningExample feedback : feedbacks) {
      jsonString += objectMapper.writeValueAsString(feedback) + "\n";
    }
    return jsonString;
  }
}
