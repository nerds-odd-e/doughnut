package com.odde.doughnut.services.ai;

import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class OpenAIChatGPTFineTuningExample {
  private @Getter List<ChatMessage> messages;
}
