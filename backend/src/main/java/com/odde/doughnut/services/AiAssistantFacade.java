package com.odde.doughnut.services;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ai.AssistantRunService;
import com.odde.doughnut.services.ai.AssistantService;
import com.odde.doughnut.services.commands.GetAiStreamCommand;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.theokanning.openai.client.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public final class AiAssistantFacade {
  private final GlobalSettingsService globalSettingsService;
  private final OpenAiApiHandler openAiApiHandler;

  public AiAssistantFacade(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      GlobalSettingsService globalSettingsService) {
    this.globalSettingsService = globalSettingsService;
    this.openAiApiHandler = new OpenAiApiHandler(openAiApi);
  }

  public SseEmitter getAiReplyForConversation(
      Conversation conversation, ConversationService conversationService, Note note) {
    AssistantService assistantService = getAssistantServiceForNotebook(note.getNotebook());
    return new GetAiStreamCommand(conversation, conversationService, note, assistantService)
        .execute();
  }

  public AssistantRunService getAssistantRunService(String threadId, String runId) {
    return new AssistantRunService(openAiApiHandler, threadId, runId);
  }

  public String suggestTopicTitle(Note note) {
    return getAssistantServiceForNotebook(note.getNotebook()).suggestTopicTitle(note);
  }

  private AssistantService getAssistantServiceForNotebook(Notebook notebook) {
    String assistantId;
    NotebookAssistant assistant = notebook.getNotebookAssistant();
    if (assistant != null) {
      assistantId = assistant.getAssistantId();
    } else {
      assistantId = globalSettingsService.defaultAssistantId().getValue();
    }
    return new AssistantService(openAiApiHandler, assistantId);
  }
}
