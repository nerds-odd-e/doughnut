package com.odde.doughnut.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import com.odde.doughnut.services.ai.OtherAiServices;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class RestAiController {

  private final OtherAiServices otherAiServices;
  private final UserModel currentUser;
  private final NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory;

  public RestAiController(
      NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory,
      OtherAiServices otherAiServices,
      UserModel currentUser) {
    this.notebookAssistantForNoteServiceFactory = notebookAssistantForNoteServiceFactory;
    this.otherAiServices = otherAiServices;
    this.currentUser = currentUser;
  }

  @GetMapping("/dummy")
  public DummyForGeneratingTypes dummyEntryToGenerateDataTypesThatAreRequiredInEventStream()
      throws HttpMediaTypeNotAcceptableException {
    throw new HttpMediaTypeNotAcceptableException("dummy");
  }

  @PostMapping("/generate-image")
  @Transactional
  public AiGeneratedImage generateImage(@RequestBody String prompt) {
    currentUser.assertLoggedIn();
    return new AiGeneratedImage(otherAiServices.getTimage(prompt));
  }

  @GetMapping("/available-gpt-models")
  public List<String> getAvailableGptModels() {
    return otherAiServices.getAvailableGptModels();
  }

  @PostMapping("/submit-tool-results/{threadId}/{runId}")
  @Transactional
  public void submitToolCallsResult(
      @PathVariable String threadId,
      @PathVariable String runId,
      @RequestBody Map<String, ToolCallResult> results)
      throws JsonProcessingException {
    currentUser.assertLoggedIn();
    // Chat completion handles tool execution inline, no need to submit results
    // If threadId/runId are synthetic (from chat completion), do nothing
    if (threadId.equals("thread-synthetic") || runId.equals("run-synthetic")) {
      return; // Chat completion - tool already executed, nothing to submit
    }
    // Legacy assistant API path (will be removed in Step 5)
    otherAiServices.resumeRun(threadId, runId).submitToolOutputs(results);
  }

  @PostMapping("/cancel-run/{threadId}/{runId}")
  @Transactional
  public void cancelRun(@PathVariable String threadId, @PathVariable String runId) {
    currentUser.assertLoggedIn();
    // Chat completion handles cancellation inline, no separate endpoint needed
    // If threadId/runId are synthetic (from chat completion), do nothing
    if (threadId.equals("thread-synthetic") || runId.equals("run-synthetic")) {
      return; // Chat completion - nothing to cancel
    }
    // Legacy assistant API path (will be removed in Step 5)
    otherAiServices.resumeRun(threadId, runId).cancelRun();
  }

  @PostMapping("/suggest-title/{note}")
  @Transactional
  public SuggestedTitleDTO suggestTitle(
      @PathVariable(value = "note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException, JsonProcessingException {
    currentUser.assertAuthorization(note);
    String title =
        notebookAssistantForNoteServiceFactory.createNoteAutomationService(note).suggestTitle();
    return new SuggestedTitleDTO(title);
  }
}
