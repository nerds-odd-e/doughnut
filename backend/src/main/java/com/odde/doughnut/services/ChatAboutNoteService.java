package com.odde.doughnut.services;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.ai.AssistantService;
import com.theokanning.openai.assistants.message.Message;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequiredArgsConstructor
public class ChatAboutNoteService {
  private final String threadId;
  private final AssistantService assistantService;

  public SseEmitter getAIReplySSE(Consumer<Message> messageConsumer) {
    return assistantService.getRunStreamAsSSE(messageConsumer, threadId);
  }

  public void createUserMessage(String userMessage) {
    assistantService.createUserMessage(userMessage, threadId);
  }

  public void sendNoteUpdateMessageIfNeeded(Note note, Conversation conversation) {
    if (conversation.getLastAiAssistantThreadSync() != null
        && note.getUpdatedAt().after(conversation.getLastAiAssistantThreadSync())) {
      assistantService.createAssistantMessage(
          "The note content has been update:\n\n%s".formatted(note.getNoteDescription()), threadId);
    }
  }

  void sendUnsentMessagesToAI(Conversation conversation) {
    List<ConversationMessage> unsynced =
        conversation.getConversationMessages().stream()
            .filter(
                msg ->
                    conversation.getLastAiAssistantThreadSync() == null
                        || msg.getCreatedAt().after(conversation.getLastAiAssistantThreadSync()))
            .filter(msg -> msg.getSender() != null)
            .toList();

    if (!unsynced.isEmpty()) {
      String combinedMessage = formatUnsentMessages(unsynced);
      createUserMessage(combinedMessage);
    } else {
      createUserMessage("just say something.");
    }
  }

  private String formatUnsentMessages(List<ConversationMessage> messages) {
    StringBuilder combined = new StringBuilder();
    for (ConversationMessage msg : messages) {
      combined.append(String.format("user `%s` says:%n", msg.getSender().getName()));
      combined.append("-----------------\n");
      combined.append(msg.getMessage());
      combined.append("\n\n");
    }
    return combined.toString();
  }
}
