package com.odde.doughnut.services.commands;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ConversationService;
import com.odde.doughnut.services.ai.AssistantService;
import com.theokanning.openai.assistants.message.Message;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class GetAiStreamCommand {
  private final Conversation conversation;
  private final ConversationService conversationService;
  private final Note note;
  private final AssistantService assistantService;

  public GetAiStreamCommand(
      Conversation conversation,
      ConversationService conversationService,
      Note note,
      AssistantService assistantService) {
    this.conversation = conversation;
    this.conversationService = conversationService;
    this.note = note;
    this.assistantService = assistantService;
  }

  public SseEmitter execute() {
    String threadId = getOrCreateThreadId();
    syncNoteUpdates(threadId);
    syncUnseenMessages(threadId);
    conversationService.updateLastAiAssistantThreadSync(conversation);

    return assistantService.getRunStreamAsSSE(
        (message -> {
          String content = extractMessageContent(message);
          conversationService.addMessageToConversation(conversation, null, content);
        }),
        threadId);
  }

  private String getOrCreateThreadId() {
    String threadId = conversation.getAiAssistantThreadId();
    if (threadId == null) {
      threadId = assistantService.createThread(note, List.of());
      conversationService.setConversationAiAssistantThreadId(conversation, threadId);
    }
    return threadId;
  }

  private void syncNoteUpdates(String threadId) {
    Timestamp lastAiAssistantThreadSync = conversation.getLastAiAssistantThreadSync();
    if (lastAiAssistantThreadSync != null && note.getUpdatedAt().after(lastAiAssistantThreadSync)) {
      assistantService.createAssistantMessage(
          "The note content has been update:\n\n%s".formatted(note.getNoteDescription()), threadId);
    }
  }

  private void syncUnseenMessages(String threadId) {
    List<ConversationMessage> unseen = conversation.getUnseenMessagesByAssistant();
    if (!unseen.isEmpty()) {
      String combinedMessage = formatUnsentMessages(unseen);
      assistantService.createUserMessage(combinedMessage, threadId);
    }
  }

  private static String formatUnsentMessages(List<ConversationMessage> messages) {
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
}
