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
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.SessionScope;
import org.springframework.web.server.ResponseStatusException;

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
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note details cannot be empty");
    }
    if (request.getPoints() == null || request.getPoints().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Points to remove cannot be empty");
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
    return promotePoint(note, request, true);
  }

  @PostMapping("/promote-point-to-sibling/{note}")
  @Transactional
  public NoteCreationResult promotePointToSibling(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody PointsRequestDTO request)
      throws UnexpectedNoAccessRightException, JsonProcessingException {
    return promotePoint(note, request, false);
  }

  private NoteCreationResult promotePoint(Note note, PointsRequestDTO request, boolean toChild)
      throws UnexpectedNoAccessRightException, JsonProcessingException {
    authorizationService.assertAuthorization(note);
    String point = getSinglePoint(request);
    var automation = notebookAssistantForNoteServiceFactory.createNoteAutomationService(note);
    PointExtractionResult aiResult =
        toChild ? automation.promotePointToChild(point) : automation.promotePointToSibling(point);
    if (aiResult == null) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "AI failed to generate extraction result");
    }
    return toChild
        ? noteConstructionService.createNoteFromPromotedPointToChild(note, aiResult)
        : noteConstructionService.createNoteFromPromotedPointToSibling(note, aiResult);
  }

  private static String getSinglePoint(PointsRequestDTO request) {
    List<String> points = request.getPoints();
    if (points == null || points.size() != 1) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "points must contain exactly one point");
    }
    return points.getFirst();
  }
}
