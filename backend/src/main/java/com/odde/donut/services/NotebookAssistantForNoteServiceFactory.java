package com.odde.donut.services;

import com.odde.donut.entities.Note;
import com.odde.donut.services.ai.AiNoteAutomationService;
import com.odde.donut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class NotebookAssistantForNoteServiceFactory {
  private final GlobalSettingsService globalSettingsService;
  private final OpenAiApiHandler openAiApiHandler;
  private final NoteQuestionGenerationService noteQuestionGenerationService;
  private final FocusContextRetrievalService focusContextRetrievalService;
  private final FocusContextMarkdownRenderer focusContextMarkdownRenderer;

  @Autowired
  public NotebookAssistantForNoteServiceFactory(
      GlobalSettingsService globalSettingsService,
      OpenAiApiHandler openAiApiHandler,
      NoteQuestionGenerationService noteQuestionGenerationService,
      FocusContextRetrievalService focusContextRetrievalService,
      FocusContextMarkdownRenderer focusContextMarkdownRenderer) {
    this.globalSettingsService = globalSettingsService;
    this.openAiApiHandler = openAiApiHandler;
    this.noteQuestionGenerationService = noteQuestionGenerationService;
    this.focusContextRetrievalService = focusContextRetrievalService;
    this.focusContextMarkdownRenderer = focusContextMarkdownRenderer;
  }

  public NoteAutomationService createNoteAutomationService(Note note) {
    AiNoteAutomationService aiNoteAutomationService =
        new AiNoteAutomationService(
            openAiApiHandler,
            globalSettingsService,
            focusContextRetrievalService,
            focusContextMarkdownRenderer,
            note);
    return new NoteAutomationService(aiNoteAutomationService);
  }

  public NoteQuestionGenerationService createNoteQuestionGenerationService(Note note) {
    return noteQuestionGenerationService;
  }
}
