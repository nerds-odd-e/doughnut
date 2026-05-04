package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.ChatCompletionNoteAutomationService;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
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
    ChatCompletionNoteAutomationService chatCompletionNoteAutomationService =
        new ChatCompletionNoteAutomationService(
            openAiApiHandler,
            globalSettingsService,
            focusContextRetrievalService,
            focusContextMarkdownRenderer,
            note);
    return new NoteAutomationService(chatCompletionNoteAutomationService);
  }

  public NoteQuestionGenerationService createNoteQuestionGenerationService(Note note) {
    return noteQuestionGenerationService;
  }
}
