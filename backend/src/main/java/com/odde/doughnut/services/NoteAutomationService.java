package com.odde.doughnut.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.services.ai.ChatCompletionNoteAutomationService;

public final class NoteAutomationService {
  private final ChatCompletionNoteAutomationService chatCompletionNoteAutomationService;

  public NoteAutomationService(
      ChatCompletionNoteAutomationService chatCompletionNoteAutomationService) {
    this.chatCompletionNoteAutomationService = chatCompletionNoteAutomationService;
  }

  public String suggestTitle() throws JsonProcessingException {
    return chatCompletionNoteAutomationService.suggestTitle();
  }

  public java.util.List<String> generateUnderstandingChecklist() throws JsonProcessingException {
    return chatCompletionNoteAutomationService.generateUnderstandingChecklist();
  }

  public com.odde.doughnut.services.ai.PointExtractionResult promotePoint(String point)
      throws JsonProcessingException {
    return chatCompletionNoteAutomationService.promotePoint(point);
  }

  public String regenerateDetailsFromPoints(java.util.List<String> points)
      throws JsonProcessingException {
    return chatCompletionNoteAutomationService.regenerateDetailsFromPoints(points);
  }

  public String removePointsAndRegenerateDetails(java.util.List<String> pointsToRemove)
      throws JsonProcessingException {
    return chatCompletionNoteAutomationService.removePointsAndRegenerateDetails(pointsToRemove);
  }
}
