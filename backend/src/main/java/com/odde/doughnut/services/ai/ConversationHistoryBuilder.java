package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.SystemMessage;
import com.theokanning.openai.completion.chat.UserMessage;
import java.util.ArrayList;
import java.util.List;

public class ConversationHistoryBuilder {
  private final ObjectMapper objectMapper;

  public ConversationHistoryBuilder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public List<ChatMessage> buildHistory(Conversation conversation) {
    List<ChatMessage> messages = new ArrayList<>();

    // Add note context as first system message
    addNoteContext(messages, conversation);

    // Add conversation messages
    for (ConversationMessage msg : conversation.getConversationMessages()) {
      if (msg.getSender() == null) {
        // AI message
        messages.add(new AssistantMessage(msg.getMessage()));
      } else {
        // User message
        messages.add(new UserMessage(msg.getMessage()));
      }
    }

    return messages;
  }

  private void addNoteContext(List<ChatMessage> messages, Conversation conversation) {
    Note note = conversation.getSubjectNote();
    if (note != null) {
      String noteDescription = note.getGraphRAGDescription(objectMapper);
      String notebookInstructions = note.getNotebookAssistantInstructions();

      // Combine note description with notebook instructions
      String systemMessageContent = noteDescription;
      if (notebookInstructions != null && !notebookInstructions.trim().isEmpty()) {
        systemMessageContent += "\n\n" + notebookInstructions;
      }

      messages.add(new SystemMessage(systemMessageContent));

      // Add additional context for recall prompts
      String additionalContext = conversation.getAdditionalContextForSubject();
      if (additionalContext != null) {
        messages.add(new SystemMessage(additionalContext));
      }

      // Add conversation instructions
      messages.add(
          new SystemMessage(
              "User is seeking for having a conversation, so don't call functions to update the note unless user asks explicitly."));
    }
  }
}
