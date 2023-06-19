package com.odde.doughnut.services;

import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.entities.json.AiSuggestionRequest;
import com.odde.doughnut.services.openAiApis.OpenAiAPIImage;
import com.odde.doughnut.services.openAiApis.OpenAiAPITextCompletion;
import com.theokanning.openai.OpenAiApi;

public class AiAdvisorService {
  private final OpenAiAPITextCompletion openAiAPITextCompletion;
  private final OpenAiAPIImage openAiAPIImage;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiAPITextCompletion = new OpenAiAPITextCompletion(openAiApi);
    openAiAPIImage = new OpenAiAPIImage(openAiApi);
  }

  public AiSuggestion getAiSuggestion(String context, AiSuggestionRequest aiSuggestionRequest) {
    return openAiAPITextCompletion.getOpenAiCompletion(context, aiSuggestionRequest);
  }

  public AiEngagingStory getEngagingStory(String prompt) {
    return new AiEngagingStory(openAiAPIImage.getOpenAiImage(prompt));
  }
}
