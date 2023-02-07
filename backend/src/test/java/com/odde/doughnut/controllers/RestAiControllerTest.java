package com.odde.doughnut.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.odde.doughnut.entities.json.AiSuggestion;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.CompletionChoice;
import com.theokanning.openai.completion.CompletionResult;
import java.util.HashMap;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class RestAiControllerTest {
  RestAiController controller;
  @Mock OpenAiService openAiService;

  HashMap<String, String> params =
      new HashMap<>() {
        {
          this.put("title", "Earth");
        }
      };

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    controller = new RestAiController(openAiService);
  }

  @Test
  void askSuggestionWithRightParams() {
    when(openAiService.createCompletion(
            argThat(
                request -> {
                  assertEquals(request.getPrompt(), "Tell me about Earth.");
                  assertEquals(request.getMaxTokens(), 100);
                  return true;
                })))
        .thenReturn(buildCompletionResult("blue planet"));
    controller.askSuggestion(params);
  }

  @Test
  void askSuggestionAndUseResponse() {
    when(openAiService.createCompletion(any())).thenReturn(buildCompletionResult("blue planet"));
    AiSuggestion aiSuggestion = controller.askSuggestion(params);
    assertEquals("blue planet", aiSuggestion.suggestion());
  }

  @NotNull
  private static CompletionResult buildCompletionResult(String text) {
    CompletionResult completionResult = new CompletionResult();
    completionResult.setChoices(
        List.of(
            new CompletionChoice() {
              {
                this.setText(text);
              }
            }));
    return completionResult;
  }
}
