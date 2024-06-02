package com.odde.doughnut.services.ai.tools;

import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.theokanning.openai.function.FunctionDefinition;
import java.util.*;

public class AiToolList {
  final Map<String, FunctionDefinition> functions = new HashMap<>();
  private String messageBody;

  public AiToolList(String message, List<FunctionDefinition> functions) {
    this.messageBody = message;
    functions.forEach(f -> this.functions.put(f.getName(), f));
  }

  public String getFirstFunctionName() {
    return functions.keySet().iterator().next();
  }

  public void addToChat(OpenAIChatRequestBuilder openAIChatRequestBuilder) {
    openAIChatRequestBuilder.functions.addAll(functions.values());
    openAIChatRequestBuilder.addUserMessage(messageBody);
  }
}
