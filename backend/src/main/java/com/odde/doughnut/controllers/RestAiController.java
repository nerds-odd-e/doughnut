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

  @PostMapping("/submit-tool-result/{threadId}/{runId}/{toolCallId}")
  @Transactional
  public void submitToolCallResult(
      @PathVariable String threadId,
      @PathVariable String runId,
      @PathVariable String toolCallId,
      @RequestBody ToolCallResult result)
      throws JsonProcessingException {
    currentUser.assertLoggedIn();
    otherAiServices.getAssistantRunService(threadId, runId).submitToolOutputs(toolCallId, result);
  }

  @PostMapping("/cancel-run/{threadId}/{runId}")
  @Transactional
  public void cancelRun(@PathVariable String threadId, @PathVariable String runId) {
    currentUser.assertLoggedIn();
    otherAiServices.getAssistantRunService(threadId, runId).cancelRun();
  }

  @PostMapping("/suggest-topic-title/{note}")
  @Transactional
  public SuggestedTopicDTO suggestTopicTitle(
      @PathVariable(value = "note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(note);
    String title = notebookAssistantForNoteServiceFactory.create(note).suggestTopicTitle(note);
    return new SuggestedTopicDTO(title);
  }
}
