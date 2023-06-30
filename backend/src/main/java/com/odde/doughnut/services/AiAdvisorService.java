package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AIGeneratedQuestion;
import com.odde.doughnut.entities.json.AiCompletion;
import com.odde.doughnut.entities.json.AiCompletionRequest;
import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.models.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.openAiApis.OpenAIChatAboutNoteRequestBuilder;
import com.odde.doughnut.services.openAiApis.OpenAiAPIChatCompletion;
import com.odde.doughnut.services.openAiApis.OpenAiAPIImage;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.apache.logging.log4j.util.Strings;

public class AiAdvisorService {
  private final OpenAiAPIChatCompletion openAiAPIChatCompletion;
  private final OpenAiAPIImage openAiAPIImage;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiAPIChatCompletion = new OpenAiAPIChatCompletion(openAiApi);
    openAiAPIImage = new OpenAiAPIImage(openAiApi);
  }

  public AiEngagingStory getEngagingStory(String prompt) {
    return new AiEngagingStory(openAiAPIImage.getOpenAiImage(prompt));
  }

  public String generateQuestionJsonString(Note note) throws QuizQuestionNotPossibleException {

    AIGeneratedQuestion openAiGenerateQuestion = getAiGeneratedQuestion(note);
    if (openAiGenerateQuestion == null || Strings.isBlank(openAiGenerateQuestion.question)) {
      throw new QuizQuestionNotPossibleException();
    }
    return new ObjectMapper().valueToTree(openAiGenerateQuestion).toString();
  }

  private AIGeneratedQuestion getAiGeneratedQuestion(Note note) {
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
        .map(AiAdvisorService::convertToAIQuestion)
        .orElse(null);
  }

  private static AIGeneratedQuestion convertToAIQuestion(JsonNode arguments) {
    try {
      return new ObjectMapper().treeToValue(arguments, AIGeneratedQuestion.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
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
