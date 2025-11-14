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
}
