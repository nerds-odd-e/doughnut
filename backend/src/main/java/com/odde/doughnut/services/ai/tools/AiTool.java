package com.odde.doughnut.services.ai.tools;

import com.theokanning.openai.completion.chat.*;

public class AiTool {
  private final String messageBody;
  private final ChatFunction function;

  public AiTool(String message, ChatFunction chatFunction) {
    this.messageBody = message;
    function = chatFunction;
  }

  public ChatFunction getFunction() {
    return function;
  }

  public String getUserRequestMessage() {
    return messageBody;
  }

  public String getFunctionName() {
    return function.getName();
  }
}
