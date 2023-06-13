package com.odde.doughnut.services;

import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
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

  public AiSuggestion getAiSuggestion(String prompt) {
    return openAiAPITextCompletion.getOpenAiCompletion(prompt);
  }

  public AiEngagingStory getEngagingStory(String prompt) {
    return new AiEngagingStory(openAiAPIImage.getOpenAiImage(prompt));
  }

  public String getProvider() {
    return "";
  }
}
