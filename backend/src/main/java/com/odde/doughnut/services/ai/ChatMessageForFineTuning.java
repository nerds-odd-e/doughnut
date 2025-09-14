package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.theokanning.openai.completion.chat.AssistantMessage;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatToolCall;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class ChatMessageForFineTuning {

  @NonNull String role;
  String content;

  @JsonProperty("function_call")
  ChatFunctionCallForFineTuning functionCall;

  public static ChatMessageForFineTuning from(ChatMessage chatMessage) {
    var chatMessageForFineTuning = new ChatMessageForFineTuning();
    chatMessageForFineTuning.role = chatMessage.getRole();
    chatMessageForFineTuning.content = chatMessage.getTextContent();
    if (chatMessage instanceof AssistantMessage assistantMessage) {
      List<ChatToolCall> toolCalls = assistantMessage.getToolCalls();
      if (toolCalls != null) {
        ChatFunctionCall function = toolCalls.get(0).getFunction();
        chatMessageForFineTuning.functionCall = ChatFunctionCallForFineTuning.from(function);
      }
    }

    return chatMessageForFineTuning;
  }
}
