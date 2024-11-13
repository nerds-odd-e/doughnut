package com.odde.doughnut.services;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ai.AssistantRunService;
import com.odde.doughnut.services.ai.AssistantService;
import com.theokanning.openai.assistants.message.Message;
import com.theokanning.openai.client.OpenAiApi;
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

  public SseEmitter getAiReplyForConversation(
      Conversation conversation, ConversationService conversationService, Note note) {
    String threadId = conversation.getAiAssistantThreadId();
    AssistantService assistantService = getAssistantServiceForNotebook(note.getNotebook());

    if (threadId == null) {
      threadId = assistantService.createThread(note, List.of());
      conversationService.setConversationAiAssistantThreadId(conversation, threadId);
    }

    Timestamp lastAiAssistantThreadSync = conversation.getLastAiAssistantThreadSync();
    if (lastAiAssistantThreadSync != null && note.getUpdatedAt().after(lastAiAssistantThreadSync)) {
      assistantService.createAssistantMessage(
          "The note content has been update:\n\n%s".formatted(note.getNoteDescription()), threadId);
    }
    List<ConversationMessage> unseen = conversation.getUnseenMessagesByAssistant();

    if (!unseen.isEmpty()) {
      String combinedMessage = formatUnsentMessages(unseen);
      assistantService.createUserMessage(combinedMessage, threadId);
    }
    conversationService.updateLastAiAssistantThreadSync(conversation);

    return assistantService.getRunStreamAsSSE(
        (message -> {
          String content = extractMessageContent(message);
          conversationService.addMessageToConversation(conversation, null, content);
        }),
        threadId);
  }

  public static String formatUnsentMessages(List<ConversationMessage> messages) {
    StringBuilder combined = new StringBuilder();
    for (ConversationMessage msg : messages) {
      combined.append(String.format("user `%s` says:%n", msg.getSender().getName()));
      combined.append("-----------------\n");
      combined.append(msg.getMessage());
      combined.append("\n\n");
    }
    return combined.toString();
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
}
