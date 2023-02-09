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

  private String getOpenAiResponse(String prompt) {
    CompletionRequest completionRequest =
        CompletionRequest.builder()
            .prompt(prompt)
            .model("text-davinci-003")
            .maxTokens(3000)
            .echo(true)
            .build();
    var choices = service.createCompletion(completionRequest).getChoices();
    return choices.stream().map(CompletionChoice::getText).collect(Collectors.joining(""));
  }

  public AiSuggestion getAiSuggestion(String item) {
    try {
      String prompt = "Tell me about " + item + ".";
      return new AiSuggestion(getOpenAiResponse(prompt).replace(prompt, "").trim());
    } catch (HttpException e) {
      if (HttpStatus.UNAUTHORIZED.value() == e.code()) {
        throw new OpenAiUnauthorizedException(e.getMessage());
      }
      return new AiSuggestion("");
    }
  }

  public AiEngagingStory getEngagingStory(List<String> items) {
    final String topics = String.join("", items);
    final String prompt = String.format("Tell me an engaging story to learn about %s", topics);
    final String story = getOpenAiResponse(prompt);

    return new AiEngagingStory(story);
  }
}
