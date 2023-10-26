package com.odde.doughnut.controllers.json;

import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class OpenAIChatGPTFineTuningExample {
  private @Getter List<ChatMessage> messages;

  public static OpenAIChatGPTFineTuningExample fromChatMessages(List<ChatMessage> messages1) {
    return new OpenAIChatGPTFineTuningExample(messages1);
  }
}
