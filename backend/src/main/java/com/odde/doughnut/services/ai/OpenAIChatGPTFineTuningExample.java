package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  public String toJsonString(ObjectMapper objectMapper) {
    try {
      return objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
