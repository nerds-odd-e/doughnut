package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiEngagingStory;
import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.services.AiAdvisorService;
import com.theokanning.openai.OpenAiService;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.annotation.SessionScope;

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

  @GetMapping("/ask-engaging-stories")
  public AiEngagingStory askEngagingStories(@RequestParam List<Note> notes) {
    List<String> titles =
        notes.stream().map(Note::getTitleAndOffSpringTitles).flatMap(List::stream).toList();
    return aiAdvisorService.getEngagingStory(titles);
  }
}
