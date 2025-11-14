package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookAssistant;
import com.odde.doughnut.services.ai.ChatCompletionNoteAutomationService;
import com.odde.doughnut.services.ai.OpenAiAssistant;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.client.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public final class NotebookAssistantForNoteServiceFactory {
  private final GlobalSettingsService globalSettingsService;
  private final OpenAiApiHandler openAiApiHandler;
  private final ObjectMapper objectMapper;

  public NotebookAssistantForNoteServiceFactory(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      GlobalSettingsService globalSettingsService,
      ObjectMapper objectMapper) {
    this.globalSettingsService = globalSettingsService;
    this.openAiApiHandler = new OpenAiApiHandler(openAiApi);
    this.objectMapper = objectMapper;
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
    return new NotebookAssistantForNoteService(assistantServiceForNotebook, note, objectMapper);
  }

  public NoteAutomationService createNoteAutomationService(Note note) {
    ChatCompletionNoteAutomationService chatCompletionNoteAutomationService =
        new ChatCompletionNoteAutomationService(
            openAiApiHandler, globalSettingsService, objectMapper, note);
    return new NoteAutomationService(chatCompletionNoteAutomationService);
  }

  public NoteQuestionGenerationService createNoteQuestionGenerationService(Note note) {
    return new NoteQuestionGenerationService(
        globalSettingsService, note, openAiApiHandler, objectMapper);
  }

  public ChatAboutNoteService createChatAboutNoteService(Note note) {
    NotebookAssistantForNoteService notebookAssistantForNoteService =
        getNotebookAssistantForNoteService(note);
    return new ChatAboutNoteService(notebookAssistantForNoteService, note);
  }
}
