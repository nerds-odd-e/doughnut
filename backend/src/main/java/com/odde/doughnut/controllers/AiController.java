package com.odde.doughnut.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import com.odde.doughnut.services.ai.NoteExtractionResult;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.services.ai.OtherAiServices;
import io.swagger.v3.oas.annotations.Operation;
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

  @PostMapping("/generate-refinement-suggestions/{note}")
  @Transactional
  public NoteRefinementLayoutDTO generateRefinementSuggestions(
      @PathVariable(value = "note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException, JsonProcessingException {
    authorizationService.assertAuthorization(note);
    String content = note.getContent();
    if (content == null || content.trim().isEmpty()) {
      return new NoteRefinementLayoutDTO(NoteRefinementLayout.empty());
    }
    try {
      NoteRefinementLayout layout =
          notebookAssistantForNoteServiceFactory
              .createNoteAutomationService(note)
              .generateRefinementSuggestions();
      return new NoteRefinementLayoutDTO(layout);
    } catch (OpenAiNotAvailableException e) {
      return new NoteRefinementLayoutDTO(NoteRefinementLayout.empty());
    }
  }

  @Operation(
      summary = "Remove refinement suggestions from note content (response only)",
      description =
          "Returns AI-regenerated note content in the response. Does not persist the note; the client must save the returned text (for example via the note update API).")
  @PostMapping("/remove-refinement-suggestion/{note}")
  @Transactional
  public RefinedContentResponseDTO removeRefinementSuggestion(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody RefinementSuggestionsRequestDTO request)
      throws UnexpectedNoAccessRightException, JsonProcessingException {

    authorizationService.assertAuthorization(note);

    String content = note.getContent();
    if (content == null || content.trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note content cannot be empty");
    }
    if (request.getSuggestions() == null || request.getSuggestions().isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Suggestions to remove cannot be empty");
    }

    String newContent =
        notebookAssistantForNoteServiceFactory
            .createNoteAutomationService(note)
            .removeSuggestionsAndRegenerateContent(request.getSuggestions());

    return new RefinedContentResponseDTO(newContent);
  }

  @PostMapping("/extract-note/{note}")
  @Transactional
  public NoteRealm extractNote(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody RefinementSuggestionsRequestDTO request)
      throws UnexpectedNoAccessRightException, JsonProcessingException {
    authorizationService.assertAuthorization(note);
    String suggestion = getSingleSuggestion(request);
    var automation = notebookAssistantForNoteServiceFactory.createNoteAutomationService(note);
    NoteExtractionResult aiResult = automation.extractNote(suggestion);
    if (aiResult == null) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "AI failed to generate extraction result");
    }
    return noteConstructionService.createNoteFromExtractedSuggestion(note, aiResult);
  }

  private static String getSingleSuggestion(RefinementSuggestionsRequestDTO request) {
    List<String> suggestions = request.getSuggestions();
    if (suggestions == null || suggestions.size() != 1) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "suggestions must contain exactly one suggestion");
    }
    return suggestions.getFirst();
  }
}
