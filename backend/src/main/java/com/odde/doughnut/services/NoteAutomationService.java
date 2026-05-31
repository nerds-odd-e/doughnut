package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.services.ai.AiNoteAutomationService;
import com.odde.doughnut.services.ai.NoteExtractionResult;
import java.util.List;

public final class NoteAutomationService {
  private final AiNoteAutomationService aiNoteAutomationService;

  public NoteAutomationService(AiNoteAutomationService aiNoteAutomationService) {
    this.aiNoteAutomationService = aiNoteAutomationService;
  }

  public String suggestTitle() throws JsonProcessingException {
    return aiNoteAutomationService.suggestTitle();
  }

  public List<String> generateRefinementSuggestions() throws JsonProcessingException {
    return aiNoteAutomationService.generateRefinementSuggestions();
  }

  public NoteExtractionResult extractNote(String suggestion) throws JsonProcessingException {
    return aiNoteAutomationService.extractNote(suggestion);
  }

  public String removeSuggestionsAndRegenerateContent(List<String> suggestionsToRemove)
      throws JsonProcessingException {
    return aiNoteAutomationService.removeSuggestionsAndRegenerateContent(suggestionsToRemove);
  }
}
