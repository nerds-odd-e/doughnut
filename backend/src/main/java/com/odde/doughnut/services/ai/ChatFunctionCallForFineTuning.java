package com.odde.doughnut.services.ai;

import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ChatFunctionCallForFineTuning {
  private String name;
  private String arguments;

  public static ChatFunctionCallForFineTuning from(
      ChatCompletionMessageFunctionToolCall.Function function) {
    return new ChatFunctionCallForFineTuning(function.name(), function.arguments());
  }
}
