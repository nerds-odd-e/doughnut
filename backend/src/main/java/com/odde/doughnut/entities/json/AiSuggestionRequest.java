package com.odde.doughnut.entities.json;

import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.util.Strings;

@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestionRequest {
  public String prompt;
  public String incompleteAssistantMessage;

  static String contextTemplate =
      "This is a personal knowledge management system, consists of notes with a title and a description, which should represent atomic concepts.\n"
          + "context: ";

  public List<ChatMessage> getChatMessages(String context) {
    List<ChatMessage> messages = new ArrayList<>();
    messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), contextTemplate + context));
    messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));
    if (!Strings.isEmpty(incompleteAssistantMessage)) {
      messages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), incompleteAssistantMessage));
    }
    return messages;
  }

  String getIncompleteMessageOrEmptyString() {
    return incompleteAssistantMessage == null ? "" : incompleteAssistantMessage;
  }
}
