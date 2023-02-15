package com.odde.doughnut.services;

import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.services.openAiApis.OpenAiApis;
import com.theokanning.openai.OpenAiService;
import java.util.List;

public class AiAdvisorService {
  private final OpenAiApis openAiApis;

  public AiAdvisorService(OpenAiService openAiService) {
    openAiApis = new OpenAiApis(openAiService);
  }

  public AiSuggestion getAiSuggestion(String item) {
    return new AiSuggestion(openAiApis.getOpenAiCompletion(item));
  }

  public AiEngagingStory getEngagingStory(List<String> items) {
    final String topics = String.join(" and ", items);
    final String prompt = String.format("Tell me an engaging story to learn about %s.", topics);

    return new AiEngagingStory(openAiApis.getOpenAiCompletion(prompt));
  }
}
