package com.odde.doughnut.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.OpenAiApi;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

  public Map<String, String> uploadFineTuningExamples() throws IOException {
    var QuestionFeedbacks = getQuestionGenerationTrainingExamples();
    var EvaluationFeedbacks = getQuestionGenerationTrainingExamples();
    return Map.of(
        "Question", uploadFineTuningExamples(QuestionFeedbacks, "Question"),
        "Evaluation", uploadFineTuningExamples(EvaluationFeedbacks, "Evaluation"));
  }

  private String uploadFineTuningExamples(
      List<OpenAIChatGPTFineTuningExample> feedbacks, String subFileName) throws IOException {
    if (feedbacks.size() < 10) {
      throw new OpenAIServiceErrorException(
          "Positive feedback cannot be less than 10.", HttpStatus.BAD_REQUEST);
    }
    String jsonString = getJsonString(feedbacks);
    var fileName = String.format("%s-%s.jsonl", subFileName, System.currentTimeMillis());
    Path file = Path.of(fileName);
    Files.createFile(file);
    Files.write(file, jsonString.getBytes(), StandardOpenOption.WRITE);
    try {
      var uploadResult = openAiApiHandler.Upload(new File(fileName));
      return uploadResult.getId();
    } catch (Exception e) {
      throw new OpenAIServiceErrorException("Upload failed.", HttpStatus.INTERNAL_SERVER_ERROR);
    } finally {
      Files.delete(file);
    }
  }

  private String getJsonString(List<OpenAIChatGPTFineTuningExample> feedbacks) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return feedbacks.stream()
        .map(x -> x.toJsonString(objectMapper))
        .collect(Collectors.joining("\n"));
  }
}
