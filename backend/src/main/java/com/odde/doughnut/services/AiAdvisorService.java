package com.odde.doughnut.services;

import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.services.openAiApis.OpenAiApis;
import com.theokanning.openai.OpenAiApi;

public class AiAdvisorService {
  private final OpenAiApis openAiApis;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiApis = new OpenAiApis(openAiApi);
  }

  public AiSuggestion getAiSuggestion(String prompt) {
    return openAiApis.getOpenAiCompletion(prompt);
  }

  public AiEngagingStory getEngagingStory(String prompt) {
    return new AiEngagingStory(openAiApis.getOpenAiImage(prompt));
  }
}
