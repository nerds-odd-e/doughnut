package com.odde.donut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.donut.controllers.dto.NoteRefinementQuestionContextDTO;
import com.odde.donut.services.ai.AiNoteAutomationService;
import com.odde.donut.services.ai.NoteExtractionResult;
import com.odde.donut.services.ai.NoteRefinementLayout;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.List;

public final class NoteAutomationService {
  private final AiNoteAutomationService aiNoteAutomationService;

  public NoteAutomationService(AiNoteAutomationService aiNoteAutomationService) {
    this.aiNoteAutomationService = aiNoteAutomationService;
  }

  public String suggestTitle() throws JsonProcessingException {
    return aiNoteAutomationService.suggestTitle();
  }

  public NoteRefinementLayout generateRefinementSuggestions(
      NoteRefinementQuestionContextDTO questionContext) throws JsonProcessingException {
    return aiNoteAutomationService.generateRefinementSuggestions(questionContext);
  }

  public StructuredResponseCreateParams<NoteRefinementLayout> buildRefinementLayoutRequest(
      NoteRefinementQuestionContextDTO questionContext) {
    return aiNoteAutomationService.buildRefinementLayoutRequest(questionContext);
  }

  public NoteExtractionResult extractNote(NoteRefinementLayout layout, List<String> selectedItemIds)
      throws JsonProcessingException {
    return aiNoteAutomationService.extractNote(layout, selectedItemIds);
  }

  public StructuredResponseCreateParams<NoteExtractionResult> buildExtractNoteRequest(
      NoteRefinementLayout layout, List<String> selectedItemIds) {
    return aiNoteAutomationService.buildExtractNoteRequest(layout, selectedItemIds);
  }

  public String removeSelectedLayoutPointsAndRegenerateContent(
      NoteRefinementLayout layout, List<String> selectedItemIds) throws JsonProcessingException {
    return aiNoteAutomationService.removeSelectedLayoutPointsAndRegenerateContent(
        layout, selectedItemIds);
  }
}
