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

  private OpenAiAssistant getAssistantServiceForNotebook(Notebook notebook) {
    String assistantId;
    NotebookAssistant assistant = notebook.getNotebookAssistant();
    if (assistant != null) {
      assistantId = assistant.getAssistantId();
    } else {
      assistantId = globalSettingsService.defaultAssistantId().getValue();
    }
    return new OpenAiAssistant(openAiApiHandler, assistantId);
  }

  private NotebookAssistantForNoteService1 getNotebookAssistantForNoteService(Note note) {
    OpenAiAssistant assistantServiceForNotebook =
        getAssistantServiceForNotebook(note.getNotebook());
    return new NotebookAssistantForNoteService1(assistantServiceForNotebook, note);
  }

  public NotebookAssistantForNoteService createNoteAutomationService(Note note) {
    NotebookAssistantForNoteService1 notebookAssistantForNoteService1 =
        getNotebookAssistantForNoteService(note);
    return new NotebookAssistantForNoteService(
        globalSettingsService, notebookAssistantForNoteService1);
  }

  public ChatAboutNoteService createChatAboutNoteService(Note note) {
    NotebookAssistantForNoteService1 notebookAssistantForNoteService =
        getNotebookAssistantForNoteService(note);
    return new ChatAboutNoteService(notebookAssistantForNoteService, note);
  }
}
