package com.odde.doughnut.services;

import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import retrofit2.HttpException;

public class AiAdvisorService {
  private final OpenAiService service;

  public AiAdvisorService(OpenAiService openAiService) {
    service = openAiService;
  }

  private String getOpenAiCompletion(String prompt) {
    CompletionRequest completionRequest =
        CompletionRequest.builder()
            .prompt(prompt)
            .model("text-davinci-003")
            // This can go higher (up to 4000 - prompt size), but openAI performance goes down
            // https://help.openai.com/en/articles/4936856-what-are-tokens-and-how-to-count-them
            .maxTokens(500)
            .build();
    List<CompletionChoice> choices = getCompletionChoices(completionRequest);
    return choices.stream().map(CompletionChoice::getText).collect(Collectors.joining("")).trim();
  }

  private List<CompletionChoice> getCompletionChoices(CompletionRequest completionRequest) {
    try {
      return service.createCompletion(completionRequest).getChoices();
    } catch (HttpException e) {
      if (HttpStatus.UNAUTHORIZED.value() == e.code()) {
        throw new OpenAiUnauthorizedException(e.getMessage());
      }
      throw e;
    }
  }

  public AiSuggestion getAiSuggestion(String item) {
    String prompt = String.format("Tell me about %s in a paragraph.", item);
    return new AiSuggestion(getOpenAiCompletion(prompt));
  }

  public AiEngagingStory getEngagingStory(List<String> items) {
    final String topics = String.join(" and ", items);
    final String prompt = String.format("Tell me an engaging story to learn about %s.", topics);
    final String story = getOpenAiCompletion(prompt);

    return new AiEngagingStory(story);
  }
}
