package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AIGeneratedQuestion;
import com.odde.doughnut.entities.json.AiCompletion;
import com.odde.doughnut.entities.json.AiCompletionRequest;
import com.odde.doughnut.models.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.openAiApis.OpenAIChatAboutNoteRequestBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.apache.logging.log4j.util.Strings;

public class AiAdvisorService {
  private final OpenAiApiHandler openAiApiHandler;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public String getImage(String prompt) {
    return openAiApiHandler.getOpenAiImage(prompt);
  }

  public String generateQuestionJsonString(Note note) throws QuizQuestionNotPossibleException {
    return generateQuestionJsonStringAvoidingPreviousQuestion(note, null);
  }

  public String generateQuestionJsonStringAvoidingPreviousQuestion(Note note, String prevQuestion)
      throws QuizQuestionNotPossibleException {
    JsonNode question = getAiGeneratedQuestion(note, prevQuestion);
    try {
      AIGeneratedQuestion aiGeneratedQuestion =
          new ObjectMapper().treeToValue(question, AIGeneratedQuestion.class);
      validateQuestion(aiGeneratedQuestion);
    } catch (JsonProcessingException e) {
      throw new QuizQuestionNotPossibleException();
    }
    return question.toString();
  }

  private static void validateQuestion(AIGeneratedQuestion question)
      throws QuizQuestionNotPossibleException {
    if (question != null) {
      if (question.stem != null && !Strings.isBlank(question.stem)) {
        return;
      }
    }
    throw new QuizQuestionNotPossibleException();
  }

  private JsonNode getAiGeneratedQuestion(Note note, String question) {
    ChatCompletionRequest chatRequest =
        new OpenAIChatAboutNoteRequestBuilder(note.getPath())
            .detailsOfNoteOfCurrentFocus(note)
            .userInstructionToGenerateQuestion(note, question)
            .maxTokens(1500)
            .build();

    return openAiApiHandler
        .chatCompletion(chatRequest)
        .map(ChatCompletionChoice::getMessage)
        .map(ChatMessage::getFunctionCall)
        .map(ChatFunctionCall::getArguments)
        .orElse(null);
  }

  public AiCompletion getAiCompletion(AiCompletionRequest aiCompletionRequest, String notePath) {
    ChatCompletionRequest chatCompletionRequest =
        new OpenAIChatAboutNoteRequestBuilder(notePath)
            .instructionForCompletion(aiCompletionRequest)
            .maxTokens(100)
            .build();
    return openAiApiHandler
        .chatCompletion(chatCompletionRequest)
        .map(aiCompletionRequest::getAiCompletion)
        .orElse(null);
  }
}
