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
import com.odde.doughnut.services.ai.NoteRefinementLayoutValidator;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.openAiApis.StructuredResponseCreateParamsSerializer;
import com.openai.models.responses.StructuredResponseCreateParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
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
  private final StructuredResponseCreateParamsSerializer paramsSerializer;

  @Autowired
  public AiController(
      NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory,
      OtherAiServices otherAiServices,
      AuthorizationService authorizationService,
      NoteConstructionService noteConstructionService,
      StructuredResponseCreateParamsSerializer paramsSerializer) {
    this.notebookAssistantForNoteServiceFactory = notebookAssistantForNoteServiceFactory;
    this.otherAiServices = otherAiServices;
    this.authorizationService = authorizationService;
    this.noteConstructionService = noteConstructionService;
    this.paramsSerializer = paramsSerializer;
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
      summary = "Remove selected layout points from note content (response only)",
      description =
          "Returns AI-regenerated note content in the response. Does not persist the note; the client must save the returned text (for example via the note update API).")
  @PostMapping("/remove-refinement-suggestion/{note}")
  @Transactional
  public RefinedContentResponseDTO removeRefinementSuggestion(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody NoteRefinementLayoutSelectionRequestDTO request)
      throws UnexpectedNoAccessRightException, JsonProcessingException {

    authorizationService.assertAuthorization(note);

    assertNoteContentNotEmpty(note);
    NoteRefinementLayout layout =
        validateLayoutSelectionRequest(request.getLayout(), request.getSelectedItemIds());

    String newContent =
        notebookAssistantForNoteServiceFactory
            .createNoteAutomationService(note)
            .removeSelectedLayoutPointsAndRegenerateContent(layout, request.getSelectedItemIds());

    return new RefinedContentResponseDTO(newContent);
  }

  @Operation(
      summary = "Preview note extraction (no persistence)",
      description =
          "Runs AI extraction for the selected layout points and returns the suggested new note title, new note content, and updated original note content without persisting any changes.")
  @PostMapping("/extract-note-preview/{note}")
  @Transactional
  public NoteExtractionResult extractNotePreview(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody NoteRefinementLayoutSelectionRequestDTO request)
      throws UnexpectedNoAccessRightException, JsonProcessingException {
    return extractNoteFromLayoutSelection(note, request);
  }

  @Operation(
      summary = "Create note from edited extraction fields",
      description =
          "Persists a new note and updates the original note using the provided extraction fields (typically from extract-note-preview, possibly edited by the user).")
  @PostMapping("/create-extracted-note/{note}")
  @Transactional
  public NoteRealm createExtractedNote(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody NoteExtractionResult request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    return noteConstructionService.createNoteFromExtractedSuggestion(note, request);
  }

  @Operation(
      summary = "Export refinement-layout AI request JSON",
      description =
          "Returns the OpenAI structured-response request body for note refinement layout generation (breakdown) without calling OpenAI.")
  @GetMapping("/export-refinement-layout-request/{note}")
  @Transactional(readOnly = true)
  public Map<String, Object> exportRefinementLayoutRequest(
      @PathVariable(value = "note") @Schema(type = "integer") Note note)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    assertNoteContentNotEmpty(note);
    StructuredResponseCreateParams<NoteRefinementLayout> params =
        notebookAssistantForNoteServiceFactory
            .createNoteAutomationService(note)
            .buildRefinementLayoutRequest();
    return paramsSerializer.toBodyMap(params);
  }

  @Operation(
      summary = "Export extract-note AI request JSON",
      description =
          "Returns the OpenAI structured-response request body for note extraction without calling OpenAI.")
  @PostMapping("/export-extract-request/{note}")
  @Transactional(readOnly = true)
  public Map<String, Object> exportExtractRequest(
      @PathVariable(value = "note") @Schema(type = "integer") Note note,
      @RequestBody NoteRefinementLayoutSelectionRequestDTO request)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(note);
    assertNoteContentNotEmpty(note);
    NoteRefinementLayout layout =
        validateLayoutSelectionRequest(request.getLayout(), request.getSelectedItemIds());
    StructuredResponseCreateParams<NoteExtractionResult> params =
        notebookAssistantForNoteServiceFactory
            .createNoteAutomationService(note)
            .buildExtractNoteRequest(layout, request.getSelectedItemIds());
    return paramsSerializer.toBodyMap(params);
  }

  private NoteExtractionResult extractNoteFromLayoutSelection(
      Note note, NoteRefinementLayoutSelectionRequestDTO request)
      throws UnexpectedNoAccessRightException, JsonProcessingException {
    authorizationService.assertAuthorization(note);
    assertNoteContentNotEmpty(note);
    NoteRefinementLayout layout =
        validateLayoutSelectionRequest(request.getLayout(), request.getSelectedItemIds());
    NoteExtractionResult aiResult =
        notebookAssistantForNoteServiceFactory
            .createNoteAutomationService(note)
            .extractNote(layout, request.getSelectedItemIds());
    if (aiResult == null) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "AI failed to generate extraction result");
    }
    return aiResult;
  }

  private static void assertNoteContentNotEmpty(Note note) {
    String content = note.getContent();
    if (content == null || content.trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Note content cannot be empty");
    }
  }

  private static NoteRefinementLayout validateLayoutSelectionRequest(
      NoteRefinementLayout layout, List<String> selectedItemIds) {
    if (!NoteRefinementLayoutValidator.isValid(layout)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "layout is invalid");
    }
    if (selectedItemIds == null || selectedItemIds.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "selectedItemIds cannot be empty");
    }
    if (NoteRefinementLayoutValidator.selectedItems(layout, selectedItemIds).size()
        != selectedItemIds.size()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "selectedItemIds must reference layout items");
    }
    return layout;
  }
}
