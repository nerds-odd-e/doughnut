package com.odde.doughnut.services.ai.tools;

import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.FunctionExecutor;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class AiTool1 {
  private final FunctionExecutor functionExecutor;

  public AiTool1(FunctionExecutor functionExecutor) {

    this.functionExecutor = functionExecutor;
  }

  public Collection<ChatFunction> getFunctions() {
    return functionExecutor.getFunctions();
  }

  public Optional<ChatMessage> execute(
      ChatFunctionCall functionCall, Function<Object, Object> executor) {
    // The API design of FunctionExecutor get an executor at the beginning, weather it is used or
    // not.
    // We choose to set the executor here, so it is only used when it is needed.
    functionExecutor.getFunctions().stream()
        .filter(f -> f.getName().equals(functionCall.getName()))
        .findFirst()
        .ifPresent(
            f -> {
              f.setExecutor(executor);
            });
    return functionExecutor.executeAndConvertToMessageSafely(functionCall);
  }
}
