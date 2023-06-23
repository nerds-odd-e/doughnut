package com.odde.doughnut.services;

import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
import com.odde.doughnut.services.openAiApis.OpenAiAPIChatCompletion;
import com.odde.doughnut.services.openAiApis.OpenAiAPIImage;
import com.theokanning.openai.OpenAiApi;

public class AiAdvisorService {
  private final OpenAiAPIChatCompletion openAiAPIChatCompletion;
  private final OpenAiAPIImage openAiAPIImage;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiAPIChatCompletion = new OpenAiAPIChatCompletion(openAiApi);
    openAiAPIImage = new OpenAiAPIImage(openAiApi);
  }

  public AiSuggestion getAiSuggestion(String context, AiSuggestionRequest aiSuggestionRequest) {
    return openAiAPIChatCompletion
        .getOpenAiCompletion(aiSuggestionRequest.getChatMessages(context), 100)
        .prependPreviousIncompleteMessage(aiSuggestionRequest);
  }

  public AiSuggestion generateQuestion(String context, String prompt) {
    AiSuggestionRequest aiSuggestionRequest = new AiSuggestionRequest(prompt, "");
    return openAiAPIChatCompletion
        .getOpenAiCompletion(aiSuggestionRequest.getChatMessages(context), 1100)
        .prependPreviousIncompleteMessage(aiSuggestionRequest);
  }

  public AiEngagingStory getEngagingStory(String prompt) {
    return new AiEngagingStory(openAiAPIImage.getOpenAiImage(prompt));
  }
}
