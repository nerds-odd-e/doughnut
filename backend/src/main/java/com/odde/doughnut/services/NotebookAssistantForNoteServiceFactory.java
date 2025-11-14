package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.ChatCompletionNoteAutomationService;
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
}
