package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AIGeneratedQuestion;
import com.odde.doughnut.entities.json.AiCompletion;
import com.odde.doughnut.entities.json.AiCompletionRequest;
import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.models.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.openAiApis.OpenAIChatAboutNoteMessageBuilder;
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
    ChatCompletionRequest chatRequest =
        new OpenAIChatAboutNoteMessageBuilder(note.getPath())
            .detailsOfNoteOfCurrentFocus(note)
            .userInstructionToGenerateQuestion()
            .buildChatCompletionRequestForGQ();
    AIGeneratedQuestion openAiGenerateQuestion =
        openAiAPIChatCompletion
            .chatCompletion(chatRequest)
            .map(ChatCompletionChoice::getMessage)
            .map(ChatMessage::getFunctionCall)
            .map(ChatFunctionCall::getArguments)
            .map(
                arguments -> {
                  try {
                    return new ObjectMapper().treeToValue(arguments, AIGeneratedQuestion.class);
                  } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                  }
                })
            .orElse(null);
    if (openAiGenerateQuestion == null || Strings.isBlank(openAiGenerateQuestion.question)) {
      throw new QuizQuestionNotPossibleException();
    }
    return new ObjectMapper().valueToTree(openAiGenerateQuestion).toString();
  }

  public AiCompletion getAiCompletion(AiCompletionRequest aiCompletionRequest, String notePath) {
    ChatCompletionRequest chatCompletionRequest =
        new OpenAIChatAboutNoteMessageBuilder(notePath)
            .instructionForCompletion(aiCompletionRequest)
            .buildChatCompletionRequest();
    return openAiAPIChatCompletion
        .chatCompletion(chatCompletionRequest)
        .map(aiCompletionRequest::getAiCompletion)
        .orElse(null);
  }
}
