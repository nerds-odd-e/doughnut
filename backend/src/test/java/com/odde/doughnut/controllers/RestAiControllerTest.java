package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.*;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.odde.doughnut.services.HttpClientAdapter;
import com.odde.doughnut.testability.TestabilitySettings;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.validation.BindException;

class RestAiControllerTest {
  RestAiController controller;
  @Mock HttpClientAdapter httpClientAdapter;
  TestabilitySettings testabilitySettings = new TestabilitySettings();

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    controller = new RestAiController(testabilitySettings, httpClientAdapter);
  }

  @Test
  void askSuggestion() throws BindException {
    HashMap params = new HashMap<>();
    params.put("title", "Animals");
    AiSuggestion aiSuggestion = controller.askSuggestion(params);
    assertEquals("Sharing the same planet as humans", aiSuggestion.suggestion);
  }
}
