package com.odde.doughnut.services;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ai.AssistantCreationService;
import com.odde.doughnut.services.ai.AssistantRunService;
import com.odde.doughnut.services.ai.AssistantService;
import com.theokanning.openai.assistants.assistant.Assistant;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.client.OpenAiApi;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public final class AiAssistantFacade {
  private final AiAssistantServiceFactory aiAssistantServiceFactory;
  private final GlobalSettingsService globalSettingsService;

  public AiAssistantFacade(
      @Qualifier("testableOpenAiApi") OpenAiApi openAiApi,
      GlobalSettingsService globalSettingsService) {
    this.aiAssistantServiceFactory = new AiAssistantServiceFactory(openAiApi);
    this.globalSettingsService = globalSettingsService;
  }

  public Assistant recreateDefaultAssistant(Timestamp currentUTCTimestamp) {
    String modelName = getModelName();
    AssistantCreationService service = aiAssistantServiceFactory.getAssistantCreationService();
    Assistant assistant = service.createDefaultAssistant(modelName, "Note details completion");
    getDefaultAssistantSettingAccessor().setKeyValue(currentUTCTimestamp, assistant.getId());
    return assistant;
  }

  public NotebookAssistant recreateNotebookAssistant(
      Timestamp currentUTCTimestamp, User creator, Notebook notebook, String additionalInstruction)
      throws IOException {
    AssistantCreationService service = aiAssistantServiceFactory.getAssistantCreationService();
    return service.recreateNotebookAssistant(
        currentUTCTimestamp, creator, notebook, additionalInstruction, getModelName());
  }

  public SseEmitter getAiReplyForConversation(
      Conversation conversation, ConversationService conversationService) {
    validateConversationForAiReply(conversation);
    ChatAboutNoteService chatService =
        setupChatServiceForConversation(conversation, conversationService);
    setupMessageHandler(conversation, chatService, conversationService);
    return chatService.getAIReplySSE();
  }

  private void validateConversationForAiReply(Conversation conversation) {
    Note note = conversation.getSubject().getNote();
    if (note == null) {
      throw new RuntimeException("Only note related conversation can have AI reply");
    }
  }

  private ChatAboutNoteService setupChatServiceForConversation(
      Conversation conversation, ConversationService conversationService) {
    Note note = conversation.getSubject().getNote();
    String threadId = conversation.getAiAssistantThreadId();
    AssistantService assistantService = getAssistantServiceForNotebook(note.getNotebook());

    if (threadId == null) {
      threadId = assistantService.createThread(note, List.of());
      conversationService.setConversationAiAssistantThreadId(conversation, threadId);
    }

    ChatAboutNoteService chatService = new ChatAboutNoteService(threadId, assistantService);
    chatService.sendNoteUpdateMessageIfNeeded(note, conversation);
    chatService.sendUnsentMessagesToAI(conversation);
    conversationService.updateLastAiAssistantThreadSync(conversation);

    return chatService;
  }

  private void setupMessageHandler(
      Conversation conversation,
      ChatAboutNoteService chatService,
      ConversationService conversationService) {
    chatService.onMessageCompleted(
        message -> {
          String content = extractMessageContent(message);
          conversationService.addMessageToConversation(conversation, null, content);
        });
  }

  private static String extractMessageContent(Message message) {
    return message.getContent().stream()
        .filter(c -> "text".equals(c.getType()))
        .map(c -> c.getText().getValue())
        .findFirst()
        .orElse("");
  }

  public AssistantRunService getAssistantRunService(String threadId, String runId) {
    return aiAssistantServiceFactory.getAssistantRunService(threadId, runId);
  }

  public String suggestTopicTitle(Note note) {
    return getAssistantServiceForNotebook(note.getNotebook()).suggestTopicTitle(note);
  }

  private String getAssistantIdForNotebook(Notebook notebook) {
    NotebookAssistant assistant = notebook.getNotebookAssistant();
    if (assistant != null) {
      return assistant.getAssistantId();
    }
    return getDefaultAssistantSettingAccessor().getValue();
  }

  private GlobalSettingsService.GlobalSettingsKeyValue getDefaultAssistantSettingAccessor() {
    return globalSettingsService.defaultAssistantId();
  }

  private AssistantService getAssistantServiceForNotebook(Notebook notebook) {
    return aiAssistantServiceFactory.getAssistantService(getAssistantIdForNotebook(notebook));
  }

  private String getModelName() {
    return globalSettingsService.globalSettingOthers().getValue();
  }
}
