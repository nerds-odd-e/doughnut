package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.services.OpenAiWrapperService;
import com.odde.doughnut.testability.TestabilitySettings;
import com.theokanning.openai.OpenAiService;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestAiController {
  OpenAiWrapperService openAiWrapperService;

  public RestAiController(
      @Qualifier("testableOpenAiService") OpenAiService openAiService,
      TestabilitySettings testabilitySettings) {}

  @PostMapping("/ask-suggestions")
  public AiSuggestion askSuggestion(@RequestBody HashMap<String, String> params)
      throws BindException {
    AiSuggestion aiSuggestion = new AiSuggestion();
    aiSuggestion.suggestion = "Sharing the same planet as humans";
    return aiSuggestion;
  }
}
