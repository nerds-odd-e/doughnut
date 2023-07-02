package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
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
    JsonNode question = getAiGeneratedQuestion(note);
    if (question != null) {
      JsonNode stem = question.get("stem");
      if (stem != null && !Strings.isBlank(stem.asText(""))) {
        return new ObjectMapper().valueToTree(question).toString();
      }
    }
    throw new QuizQuestionNotPossibleException();
  }

  private JsonNode getAiGeneratedQuestion(Note note) {
    ChatCompletionRequest chatRequest =
        new OpenAIChatAboutNoteRequestBuilder(note.getPath())
            .detailsOfNoteOfCurrentFocus(note)
            .userInstructionToGenerateQuestion()
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
