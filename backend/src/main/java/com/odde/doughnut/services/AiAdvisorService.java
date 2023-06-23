package com.odde.doughnut.services;

import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiQuestionTemporary;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import com.odde.doughnut.services.openAiApis.OpenAiAPIChatCompletion;
import com.odde.doughnut.services.openAiApis.OpenAiAPIImage;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;

public class AiAdvisorService {
  private final OpenAiAPIChatCompletion openAiAPIChatCompletion;
  private final OpenAiAPIImage openAiAPIImage;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiAPIChatCompletion = new OpenAiAPIChatCompletion(openAiApi);
    openAiAPIImage = new OpenAiAPIImage(openAiApi);
  }

  public AiSuggestion getAiSuggestion(
      List<ChatMessage> messages, String incompleteAssistantMessage) {
    return openAiAPIChatCompletion
        .getOpenAiCompletion(messages, 100)
        .prependPreviousIncompleteMessage(incompleteAssistantMessage);
  }

  public AiQuestionTemporary generateQuestion(List<ChatMessage> messages) {
    AiSuggestion openAiCompletion = openAiAPIChatCompletion.getOpenAiCompletion(messages, 1100);
    QuizQuestionViewedByUser quizQuestionViewedByUser = null;
    return new AiQuestionTemporary(openAiCompletion.getSuggestion(), quizQuestionViewedByUser);
  }

  public AiEngagingStory getEngagingStory(String prompt) {
    return new AiEngagingStory(openAiAPIImage.getOpenAiImage(prompt));
  }
}
