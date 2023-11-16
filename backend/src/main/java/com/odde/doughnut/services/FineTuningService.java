package com.odde.doughnut.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.SuggestedQuestionForFineTuning;
import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.fine_tuning.FineTuningJobRequest;
import com.theokanning.openai.fine_tuning.Hyperparameters;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
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

  private String uploadFineTuningExamples(List<? extends Object> examples, String subFileName)
      throws IOException {
    if (examples.size() < 10) {
      throw new OpenAIServiceErrorException(
          "Positive feedback cannot be less than 10.", HttpStatus.BAD_REQUEST);
    }
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    String jsonString =
        examples.stream()
            .map(
                x -> {
                  try {
                    return objectMapper.writeValueAsString(x);
                  } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                  }
                })
            .collect(Collectors.joining("\n"));

    File tempFile = File.createTempFile(subFileName, ".jsonl");
    try {
      // Write to the temporary file
      Files.write(tempFile.toPath(), jsonString.getBytes(), StandardOpenOption.WRITE);

      // Upload the file
      var uploadResult = openAiApiHandler.Upload(tempFile);
      return uploadResult.getId();
    } catch (Exception e) {
      throw new OpenAIServiceErrorException("Upload failed.", HttpStatus.INTERNAL_SERVER_ERROR);
    } finally {
      tempFile.delete();
    }
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
    var questionFeedbacks = getQuestionGenerationTrainingExamples();
    var evaluationFeedbacks = getQuestionEvaluationTrainingExamples();
    String question = uploadFineTuningExamples(questionFeedbacks, "Question");
    String evaluation = uploadFineTuningExamples(evaluationFeedbacks, "Evaluation");

    try {
      triggerFineTune(question);
      triggerFineTune(evaluation);
    } catch (Exception e) {
      throw new OpenAIServiceErrorException("Training failed.", HttpStatus.BAD_REQUEST);
    }
  }
}
