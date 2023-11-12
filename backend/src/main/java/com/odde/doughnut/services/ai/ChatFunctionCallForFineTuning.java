package com.odde.doughnut.services.ai;

import com.theokanning.openai.completion.chat.ChatFunctionCall;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ChatFunctionCallForFineTuning {
  private String name;
  private String arguments;

  public static ChatFunctionCallForFineTuning from(ChatFunctionCall functionCall) {
    return new ChatFunctionCallForFineTuning(
        functionCall.getName(), functionCall.getArguments().toString());
  }
}
