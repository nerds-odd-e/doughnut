package com.odde.doughnut.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.controllers.dto.IgnorePointsRequestDTO;
import com.odde.doughnut.controllers.dto.IgnorePointsResponseDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.ai.PointExtractionResult;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;

@RestController
@SessionScope
@RequestMapping("/api/ai")
public class AiController {

  private final OtherAiServices otherAiServices;
  private final NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory;
  private final AuthorizationService authorizationService;
  private final NoteConstructionService noteConstructionService;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;

  @Autowired
  public AiController(
      NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory,
      OtherAiServices otherAiServices,
      AuthorizationService authorizationService,
      NoteConstructionService noteConstructionService,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings) {
    this.notebookAssistantForNoteServiceFactory = notebookAssistantForNoteServiceFactory;
    this.otherAiServices = otherAiServices;
    this.authorizationService = authorizationService;
    this.noteConstructionService = noteConstructionService;
    this.testabilitySettings = testabilitySettings;
    this.entityPersister = entityPersister;
  }

  @GetMapping("/dummy")
  public DummyForGeneratingTypes dummyEntryToGenerateDataTypesThatAreRequiredInEventStream()
      throws HttpMediaTypeNotAcceptableException {
    throw new HttpMediaTypeNotAcceptableException("dummy");
  }

  @PostMapping("/generate-image")
  @Transactional
  public AiGeneratedImage generateImage(@RequestBody String prompt) {
    authorizationService.assertLoggedIn();
    return new AiGeneratedImage(otherAiServices.getTimage(prompt));
  }

  @GetMapping("/available-gpt-models")
  public List<String> getAvailableGptModels() {
    return otherAiServices.getAvailableGptModels();
  }

  @PostMapping("/suggest-title/{note}")
  @Transactional
  public SuggestedTitleDTO suggestTitle(
      @PathVariable(value = "note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException, JsonProcessingException {
    authorizationService.assertAuthorization(note);
    String title =
        notebookAssistantForNoteServiceFactory.createNoteAutomationService(note).suggestTitle();
    return new SuggestedTitleDTO(title);
  }

  @PostMapping("/generate-understanding-checklist/{note}")
  @Transactional
  public UnderstandingChecklistDTO generateUnderstandingChecklist(
      @PathVariable(value = "note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException, JsonProcessingException {
    authorizationService.assertAuthorization(note);
    String details = note.getDetails();
    if (details == null || details.trim().isEmpty()) {
      return new UnderstandingChecklistDTO(List.of());
    }
    List<String> points =
        notebookAssistantForNoteServiceFactory
            .createNoteAutomationService(note)
            .generateUnderstandingChecklist();
    return new UnderstandingChecklistDTO(points);
  }

  @PostMapping("/remove-point-from-note/{note}")
  @Transactional
  public RemovePointsResponseDTO removePointFromNote(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody RemovePointsRequestDTO request)
      throws UnexpectedNoAccessRightException, JsonProcessingException {

    authorizationService.assertAuthorization(note);

    String details = note.getDetails();
    if (details == null || details.trim().isEmpty()) {
      return new RemovePointsResponseDTO(details);
    }
    if (request.getPoints() == null || request.getPoints().isEmpty()) {
      return new RemovePointsResponseDTO(details);
    }

    String newDetails =
        notebookAssistantForNoteServiceFactory
            .createNoteAutomationService(note)
            .removePointsAndRegenerateDetails(request.getPoints());

    note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
    note.setDetails(newDetails);
    entityPersister.save(note);

    return new RemovePointsResponseDTO(newDetails);
  }

  @PostMapping("/ignore-points/{note}")
  @Transactional
  public IgnorePointsResponseDTO ignorePoints(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody IgnorePointsRequestDTO request)
      throws UnexpectedNoAccessRightException, JsonProcessingException {
    authorizationService.assertAuthorization(note);

    if (request.getPoints() != null && !request.getPoints().isEmpty()) {
      String ignoredPointsJson =
          new ObjectMapperConfig().objectMapper().writeValueAsString(request.getPoints());
      note.setIgnoredPoints(ignoredPointsJson);
      note.setUpdatedAt(testabilitySettings.getCurrentUTCTimestamp());
      entityPersister.save(note);
    }

    return new IgnorePointsResponseDTO(true);
  }

  @PostMapping("/promote-point/{note}")
  @Transactional
  public PromotePointResponseDTO promotePoint(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody PromotePointRequestDTO request)
      throws UnexpectedNoAccessRightException, JsonProcessingException {

    authorizationService.assertAuthorization(note);

    // 1. Call AI to generate result
    PointExtractionResult result =
        notebookAssistantForNoteServiceFactory
            .createNoteAutomationService(note)
            .promotePoint(request.getPoint());

    if (result == null) {
      throw new RuntimeException("AI failed to generate extraction result");
    }

    // 2. Create new note under the specified parent (authorization checked in service)
    User user = authorizationService.getCurrentUser();
    Note newNote =
        noteConstructionService.createNoteUnderParentId(
            request.getParentNoteId(), result.newNoteTitle);
    newNote.setDetails(result.newNoteDetails);

    // 3. Update original note's details
    note.setDetails(result.updatedParentDetails);

    // 4. Return result
    return new PromotePointResponseDTO(newNote.toNoteRealm(user), note.toNoteRealm(user));
  }
}
