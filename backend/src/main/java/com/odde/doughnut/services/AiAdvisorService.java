package com.odde.doughnut.services;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.odde.doughnut.controllers.json.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.ai.*;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
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

  public AiCompletionResponse getAiCompletion(
      AiCompletionParams aiCompletionParams, Note note, String modelName) {
    ChatCompletionRequest chatCompletionRequest =
        new OpenAIChatAboutNoteRequestBuilder1(modelName)
            .contentOfNoteOfCurrentFocus(note)
            .instructionForDetailsCompletion(aiCompletionParams)
            .maxTokens(150)
            .build();

    ChatFunctionCall chatFunctionCall =
        openAiApiHandler.getFunctionCall(chatCompletionRequest).orElseThrow();
    boolean isClarifyingQuestion =
        chatFunctionCall
            .getName()
            .equals(OpenAIChatAboutNoteRequestBuilder.askClarificationQuestion);
    AiCompletionResponse result = new AiCompletionResponse();
    if (isClarifyingQuestion) {
      result.setFinishReason("question");
      ClarifyingQuestion result1;
      JsonNode jsonNode = chatFunctionCall.getArguments();
      try {
        result1 = defaultObjectMapper().treeToValue(jsonNode, ClarifyingQuestion.class);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
      result.setClarifyingQuestion(result1);
      aiCompletionParams.getClarifyingQuestionAndAnswers().forEach(result::addClarifyingHistory);
      return result;
    }
    String result1;
    JsonNode jsonNode = chatFunctionCall.getArguments();
    try {
      NoteDetailsCompletion noteDetailsCompletion =
          defaultObjectMapper().treeToValue(jsonNode, NoteDetailsCompletion.class);
      aiCompletionParams.setDetailsToComplete(
          aiCompletionParams.getDetailsToComplete() + noteDetailsCompletion.completion);
      result1 = aiCompletionParams.getDetailsToComplete();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    String content = result1;
    result.setMoreCompleteContent(content);
    result.setFinishReason("stop");
    return result;
  }

  public String chatWithAi(Note note, String userMessage, String modelName) {
    ChatCompletionRequest chatCompletionRequest =
        new OpenAIChatAboutNoteRequestBuilder1(modelName)
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
