package com.odde.doughnut.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.ai.PointExtractionResult;
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

  @Autowired
  public AiController(
      NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory,
      OtherAiServices otherAiServices,
      AuthorizationService authorizationService,
      NoteConstructionService noteConstructionService) {
    this.notebookAssistantForNoteServiceFactory = notebookAssistantForNoteServiceFactory;
    this.otherAiServices = otherAiServices;
    this.authorizationService = authorizationService;
    this.noteConstructionService = noteConstructionService;
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
    try {
      List<String> points =
          notebookAssistantForNoteServiceFactory
              .createNoteAutomationService(note)
              .generateUnderstandingChecklist();
      return new UnderstandingChecklistDTO(points);
    } catch (OpenAiNotAvailableException e) {
      return new UnderstandingChecklistDTO(List.of());
    }
  }

  @PostMapping("/remove-point-from-note/{note}")
  @Transactional
  public RemovePointsResponseDTO removePointFromNote(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody PointsRequestDTO request)
      throws UnexpectedNoAccessRightException, JsonProcessingException {

    authorizationService.assertAuthorization(note);

    String details = note.getDetails();
    if (details == null || details.trim().isEmpty()) {
      throw new IllegalArgumentException("Note details cannot be empty");
    }
    if (request.getPoints() == null || request.getPoints().isEmpty()) {
      throw new IllegalArgumentException("Points to remove cannot be empty");
    }

    String newDetails =
        notebookAssistantForNoteServiceFactory
            .createNoteAutomationService(note)
            .removePointsAndRegenerateDetails(request.getPoints());

    return new RemovePointsResponseDTO(newDetails);
  }

  @PostMapping("/promote-point-to-child/{note}")
  @Transactional
  public NoteCreationResult promotePointToChild(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody PointsRequestDTO request)
      throws UnexpectedNoAccessRightException, JsonProcessingException {

    authorizationService.assertAuthorization(note);

    String point = getSinglePoint(request);
    PointExtractionResult aiResult =
        notebookAssistantForNoteServiceFactory
            .createNoteAutomationService(note)
            .promotePointToChild(point);

    if (aiResult == null) {
      throw new RuntimeException("AI failed to generate extraction result");
    }

    return noteConstructionService.createNoteFromPromotedPointToChild(note, aiResult);
  }

  @PostMapping("/promote-point-to-sibling/{note}")
  @Transactional
  public NoteCreationResult promotePointToSibling(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody PointsRequestDTO request)
      throws UnexpectedNoAccessRightException, JsonProcessingException {

    authorizationService.assertAuthorization(note);

    String point = getSinglePoint(request);
    PointExtractionResult aiResult =
        notebookAssistantForNoteServiceFactory
            .createNoteAutomationService(note)
            .promotePointToSibling(point);

    if (aiResult == null) {
      throw new RuntimeException("AI failed to generate extraction result");
    }

    return noteConstructionService.createNoteFromPromotedPointToSibling(note, aiResult);
  }

  private static String getSinglePoint(PointsRequestDTO request) {
    if (request.getPoints() == null || request.getPoints().isEmpty()) {
      throw new IllegalArgumentException("points must contain exactly one point");
    }
    return request.getPoints().getFirst();
  }
}
