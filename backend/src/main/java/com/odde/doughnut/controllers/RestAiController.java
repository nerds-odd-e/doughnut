package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.services.AiAdvisorService;
import com.theokanning.openai.OpenAiService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;
import reactor.core.publisher.Flux;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestAiController {
  AiAdvisorService aiAdvisorService;

  public RestAiController(@Qualifier("testableOpenAiService") OpenAiService openAiService) {
    aiAdvisorService = new AiAdvisorService(openAiService);
  }

  @PostMapping("/ask-suggestions")
  public AiSuggestion askSuggestion(@RequestBody HashMap<String, String> params) {
    return aiAdvisorService.getAiSuggestion(params.get("title"));
  }

  @GetMapping(value = "/stream-suggestions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<AiSuggestion> streamSuggestions() {
    return Flux.just(aiAdvisorService.getAiSuggestion("Harry Potter"));
  }

  @GetMapping("/ask-story/{note}")
  public AiStory askStory(@PathVariable Note note) {
    List<String> titles = new ArrayList<>();
    titles.add(note.getTitle());
    if (note.getChildren().size() == 1) {
      titles.add(note.getChildren().get(0).getTitle());
    }
    return aiAdvisorService.getEngagingStory(titles);
  }
}
