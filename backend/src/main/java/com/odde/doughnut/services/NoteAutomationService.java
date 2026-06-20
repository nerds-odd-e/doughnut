package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.services.ai.AiNoteAutomationService;
import com.odde.doughnut.services.ai.NoteExtractionResult;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import java.util.List;

public final class NoteAutomationService {
  private final AiNoteAutomationService aiNoteAutomationService;

  public NoteAutomationService(AiNoteAutomationService aiNoteAutomationService) {
    this.aiNoteAutomationService = aiNoteAutomationService;
  }

  public String suggestTitle() throws JsonProcessingException {
    return aiNoteAutomationService.suggestTitle();
  }

  public NoteRefinementLayout generateRefinementSuggestions() throws JsonProcessingException {
    return aiNoteAutomationService.generateRefinementSuggestions();
  }

  public NoteExtractionResult extractNote(NoteRefinementLayout layout, List<String> selectedItemIds)
      throws JsonProcessingException {
    return aiNoteAutomationService.extractNote(layout, selectedItemIds);
  }

  public String removeSelectedLayoutPointsAndRegenerateContent(
      NoteRefinementLayout layout, List<String> selectedItemIds) throws JsonProcessingException {
    return aiNoteAutomationService.removeSelectedLayoutPointsAndRegenerateContent(
        layout, selectedItemIds);
  }
}
