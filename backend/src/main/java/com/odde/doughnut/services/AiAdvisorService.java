package com.odde.doughnut.services;

import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.services.openAiApis.OpenAiApis;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import java.util.List;

public class AiAdvisorService {
  private final OpenAiApis openAiApis;

  public AiAdvisorService(OpenAiService openAiService, OpenAiApi openAiApi) {
    openAiApis = new OpenAiApis(openAiService, openAiApi);
  }

  public AiSuggestion getAiSuggestion(String prompt) {
    return new AiSuggestion(openAiApis.getOpenAiCompletion(prompt));
  }

  public AiEngagingStory getEngagingStory(List<String> items) {
    final String topics = String.join(" and ", items);
    final String prompt = String.format("Tell me an engaging story to learn about %s.", topics);

    return new AiEngagingStory(openAiApis.getOpenAiCompletion(prompt));
  }
}
