package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Conversation;
import com.odde.doughnut.entities.ConversationMessage;
import com.odde.doughnut.entities.Note;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import java.util.ArrayList;
import java.util.List;

public class ConversationHistoryBuilder {
  private final ObjectMapper objectMapper;

  public ConversationHistoryBuilder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public List<ChatCompletionMessageParam> buildHistory(Conversation conversation) {
    List<ChatCompletionMessageParam> messages = new ArrayList<>();

    // Add note context as first system message
    addNoteContext(messages, conversation);

    // Add conversation messages
    for (ConversationMessage msg : conversation.getConversationMessages()) {
      if (msg.getSender() == null) {
        // AI message
        messages.add(
            ChatCompletionMessageParam.ofAssistant(
                ChatCompletionAssistantMessageParam.builder().content(msg.getMessage()).build()));
      } else {
        // User message
        messages.add(
            ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder().content(msg.getMessage()).build()));
      }
    }

    return messages;
  }

  private void addNoteContext(
      List<ChatCompletionMessageParam> messages, Conversation conversation) {
    Note note = conversation.getSubjectNote();
    if (note != null) {
      String noteDescription = note.getGraphRAGDescription(objectMapper);
      String notebookInstructions = note.getNotebookAssistantInstructions();

      // Combine note description with notebook instructions
      String systemMessageContent = noteDescription;
      if (notebookInstructions != null && !notebookInstructions.trim().isEmpty()) {
        systemMessageContent += "\n\n" + notebookInstructions;
      }

      messages.add(
          ChatCompletionMessageParam.ofSystem(
              ChatCompletionSystemMessageParam.builder().content(systemMessageContent).build()));

      // Add additional context for recall prompts
      String additionalContext = conversation.getAdditionalContextForSubject();
      if (additionalContext != null) {
        messages.add(
            ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder().content(additionalContext).build()));
      }

      // Add conversation instructions
      messages.add(
          ChatCompletionMessageParam.ofSystem(
              ChatCompletionSystemMessageParam.builder()
                  .content(
                      "User is seeking for having a conversation, so don't call functions to update the note unless user asks explicitly.")
                  .build()));
    }
  }
}
