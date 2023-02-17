package com.odde.doughnut.services;

import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.services.openAiApis.OpenAiApis;
import com.theokanning.openai.OpenAiApi;
import java.util.List;

public class AiAdvisorService {
  private final OpenAiApis openAiApis;

  public AiAdvisorService(OpenAiApi openAiApi) {
    openAiApis = new OpenAiApis(openAiApi);
  }

  public AiSuggestion getAiSuggestion(String prompt) {
    return openAiApis.getOpenAiCompletion(prompt).map(AiSuggestion::new).blockFirst();
  }

  public AiEngagingStory getEngagingStory(List<String> items) {
    final String topics = String.join(" and ", items);
    final String prompt = String.format("Tell me an engaging story to learn about %s.", topics);

    return new AiEngagingStory(openAiApis.getOpenAiCompletion(prompt).blockLast());
  }
}
