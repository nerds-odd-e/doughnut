package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAssistant;
import com.odde.doughnut.services.ai.OpenAiAssistant;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.client.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public final class NotebookAssistantForNoteServiceFactory {
  private final GlobalSettingsService globalSettingsService;
  private final OpenAiApiHandler openAiApiHandler;

  public NotebookAssistantForNoteServiceFactory(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      GlobalSettingsService globalSettingsService) {
    this.globalSettingsService = globalSettingsService;
    this.openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  private String getNotebookAssistantId(Notebook notebook) {
    NotebookAssistant assistant = notebook.getNotebookAssistant();
    if (assistant != null) {
      return assistant.getAssistantId();
    }
    return null;
  }

  private NotebookAssistantForNoteService getNotebookAssistantForNoteService(Note note) {
    String assistantId = getNotebookAssistantId(note.getNotebook());
    if (assistantId != null) {
      return getServiceByAssistantId(assistantId, note);
    }
    return getDefaultAssistantForNoteService(note);
  }

  private NotebookAssistantForNoteService getDefaultAssistantForNoteService(Note note) {
    return getServiceByAssistantId(globalSettingsService.defaultAssistantId().getValue(), note);
  }

  private NotebookAssistantForNoteService getServiceByAssistantId(String assistantId, Note note) {
    OpenAiAssistant assistantServiceForNotebook =
        new OpenAiAssistant(openAiApiHandler, assistantId);
    return new NotebookAssistantForNoteService(assistantServiceForNotebook, note);
  }

  public NoteAutomationService createNoteAutomationService(Note note) {
    NotebookAssistantForNoteService notebookAssistantForNoteService =
        getDefaultAssistantForNoteService(note);
    return new NoteAutomationService(notebookAssistantForNoteService);
  }

  public NoteQuestionGenerationService createNoteQuestionGenerationService(Note note) {
    return new NoteQuestionGenerationService(globalSettingsService, note, openAiApiHandler);
  }

  public ChatAboutNoteService createChatAboutNoteService(Note note) {
    NotebookAssistantForNoteService notebookAssistantForNoteService =
        getNotebookAssistantForNoteService(note);
    return new ChatAboutNoteService(notebookAssistantForNoteService, note);
  }
}
