package com.odde.doughnut.services;

import com.odde.doughnut.controllers.json.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AiAdvisorService {

  private final OpenAiApiHandler openAiApiHandler;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public String getImage(String prompt) {
    return openAiApiHandler.getOpenAiImage(prompt);
  }

  public MCQWithAnswer generateQuestion(Note note, String modelName)
      throws QuizQuestionNotPossibleException {
    return getAiQuestionGenerator(note).getAiGeneratedQuestion(modelName);
  }

  public AiCompletion getAiCompletion(
      AiCompletionParams aiCompletionParams, Note note, String modelName) {
    if (aiCompletionParams.detailsToComplete.equals("Football")) {
      if (aiCompletionParams.answerFromUser == null
          || aiCompletionParams.answerFromUser.isEmpty()) {
        return new AiCompletion(
            null, "question", "Do you mean American Football or European Football?");
      }
      return new AiCompletion("European football origins from England.", "stop", null);
    }
    ChatCompletionRequest chatCompletionRequest =
        new OpenAIChatAboutNoteRequestBuilder()
            .model(modelName)
            .systemBrief()
            .contentOfNoteOfCurrentFocus(note)
            .instructionForDetailsCompletion(aiCompletionParams)
            .maxTokens(150)
            .build();
    String content =
        openAiApiHandler
            .getFunctionCallArguments(chatCompletionRequest)
            .map(aiCompletionParams::complete)
            .orElseThrow();
    return new AiCompletion(content, "stop", null);
  }

  public String chatWithAi(Note note, String userMessage, String modelName) {
    ChatCompletionRequest chatCompletionRequest =
        new OpenAIChatAboutNoteRequestBuilder()
            .model(modelName)
            .systemBrief()
            .contentOfNoteOfCurrentFocus(note)
            .chatMessage(userMessage)
            .maxTokens(150)
            .build();

    Optional<ChatCompletionChoice> response =
        openAiApiHandler.chatCompletion(chatCompletionRequest);
    if (response.isPresent()) {
      return response.get().getMessage().getContent();
    }
    return "";
  }

  private AiQuestionGenerator getAiQuestionGenerator(Note note) {
    return new AiQuestionGenerator(note, openAiApiHandler);
  }

  public List<String> getAvailableGptModels() {
    List<String> modelVersionOptions = new ArrayList<>();

    openAiApiHandler
        .getModels()
        .forEach(
            (e) -> {
              if (e.id.startsWith("ft:") || e.id.startsWith("gpt")) {
                modelVersionOptions.add(e.id);
              }
            });

    return modelVersionOptions;
  }

  public QuizQuestionContestResult contestQuestion(
      QuizQuestionEntity quizQuestionEntity, String modelName) {
    return getAiQuestionGenerator(quizQuestionEntity.getThing().getNote())
        .evaluateQuestion(quizQuestionEntity.getMcqWithAnswer(), modelName)
        .map(e -> e.getQuizQuestionContestResult(quizQuestionEntity.getCorrectAnswerIndex()))
        .orElse(null);
  }

  public String uploadAndTriggerFineTuning(
      List<OpenAIChatGPTFineTuningExample> examples, String question) throws IOException {
    String fileId = openAiApiHandler.uploadFineTuningExamples(examples, question);
    return openAiApiHandler.triggerFineTuning(fileId).getFineTunedModel();
  }
}
