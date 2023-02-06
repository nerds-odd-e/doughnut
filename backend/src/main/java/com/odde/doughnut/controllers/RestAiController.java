package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.services.OpenAiWrapperService;
import com.theokanning.openai.OpenAiService;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestAiController {
  OpenAiWrapperService openAiWrapperService;

  public RestAiController(@Qualifier("testableOpenAiService") OpenAiService openAiService) {
    openAiWrapperService = new OpenAiWrapperService(openAiService);
  }

  @PostMapping("/ask-suggestions")
  public AiSuggestion askSuggestion(@RequestBody HashMap<String, String> params) {
    AiSuggestion aiSuggestion = new AiSuggestion();
    aiSuggestion.suggestion = openAiWrapperService.getSuggestedDescription(params.get("title"));
    return aiSuggestion;
  }
}
