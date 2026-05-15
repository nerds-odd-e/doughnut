package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.services.ai.AiNoteAutomationService;

public final class NoteAutomationService {
  private final AiNoteAutomationService aiNoteAutomationService;

  public NoteAutomationService(AiNoteAutomationService aiNoteAutomationService) {
    this.aiNoteAutomationService = aiNoteAutomationService;
  }

  public String suggestTitle() throws JsonProcessingException {
    return aiNoteAutomationService.suggestTitle();
  }

  public java.util.List<String> generateUnderstandingChecklist() throws JsonProcessingException {
    return aiNoteAutomationService.generateUnderstandingChecklist();
  }

  public com.odde.doughnut.services.ai.PointExtractionResult promotePointToSibling(String point)
      throws JsonProcessingException {
    return aiNoteAutomationService.promotePointToSibling(point);
  }

  public String removePointsAndRegenerateContent(java.util.List<String> pointsToRemove)
      throws JsonProcessingException {
    return aiNoteAutomationService.removePointsAndRegenerateContent(pointsToRemove);
  }
}
