package com.odde.doughnut.services.ai.tools;

import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.theokanning.openai.completion.chat.*;

public class AiTool<T> {
  public final String functionName;
  private final Class<T> type;
  private final String description;
  private final String messageBody;

  public AiTool(Class<T> type, String functionName, String description, String message) {
    this.type = type;
    this.functionName = functionName;
    this.description = description;
    this.messageBody = message;
  }

  public void addToolToChatMessages(OpenAIChatRequestBuilder openAIChatRequestBuilder) {
    openAIChatRequestBuilder.functions.add(
        ChatFunction.builder()
            .name(functionName)
            .description(description)
            .executor(type, null)
            .build());

    openAIChatRequestBuilder.addUserMessage(messageBody);
  }

  public void addFunctionCallResultToMessages(
      OpenAIChatRequestBuilder openAIChatRequestBuilder, T argument) {
    openAIChatRequestBuilder.addFunctionCallMessage(argument, functionName);
  }
}
