package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiCompletion;
import com.odde.doughnut.entities.json.AiCompletionRequest;
import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.models.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.openAiApis.OpenAIChatAboutNoteRequestBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiAPIImage;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandlerBase;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.apache.logging.log4j.util.Strings;

public class AiAdvisorService {
  private final OpenAiApiHandlerBase openAiAPIChatCompletion;
  private final OpenAiAPIImage openAiAPIImage;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiAPIChatCompletion = new OpenAiApiHandlerBase(openAiApi);
    openAiAPIImage = new OpenAiAPIImage(openAiApi);
  }

  public AiEngagingStory getEngagingStory(String prompt) {
    return new AiEngagingStory(openAiAPIImage.getOpenAiImage(prompt));
  }

  public String generateQuestionJsonString(Note note) throws QuizQuestionNotPossibleException {
    JsonNode question = getAiGeneratedQuestion(note);
    if (question == null || Strings.isBlank(question.get("question").asText(""))) {
      throw new QuizQuestionNotPossibleException();
    }
    return new ObjectMapper().valueToTree(question).toString();
  }

  private JsonNode getAiGeneratedQuestion(Note note) {
    ChatCompletionRequest chatRequest =
        new OpenAIChatAboutNoteRequestBuilder(note.getPath())
            .detailsOfNoteOfCurrentFocus(note)
            .userInstructionToGenerateQuestion()
            .maxTokens(1500)
            .build();
    return openAiAPIChatCompletion
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
    return openAiAPIChatCompletion
        .chatCompletion(chatCompletionRequest)
        .map(aiCompletionRequest::getAiCompletion)
        .orElse(null);
  }
}
