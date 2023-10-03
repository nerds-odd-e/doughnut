package com.odde.doughnut.controllers.json;

import com.theokanning.openai.completion.chat.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class SimplifiedOpenAIChatMessage {
  private String role;
  private String content;

  public static SimplifiedOpenAIChatMessage fromOpenAIChatMessage(ChatMessage chatMessage) {
    return new SimplifiedOpenAIChatMessage(chatMessage.getRole(), chatMessage.getContent());
  }
}
