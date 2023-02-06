package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.testability.TestabilitySettings;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestAiController {

  public RestAiController(
      TestabilitySettings testabilitySettings, HttpClientAdapter httpClientAdapter) {}

  @PostMapping("/ask-suggestions")
  public AiSuggestion askSuggestion() throws BindException {
    AiSuggestion aiSuggestion = new AiSuggestion();
    aiSuggestion.suggestion = "Sharing the same planet as humans";
    return aiSuggestion;
  }
}
