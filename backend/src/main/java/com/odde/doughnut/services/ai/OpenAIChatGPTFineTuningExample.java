package com.odde.doughnut.services.ai;

import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class OpenAIChatGPTFineTuningExample {
  private @Getter List<ChatMessageForFineTuning> messages;

  public static OpenAIChatGPTFineTuningExample from(List<ChatMessage> messages) {
    return new OpenAIChatGPTFineTuningExample(
        messages.stream().map(ChatMessageForFineTuning::from).toList());
  }
}
