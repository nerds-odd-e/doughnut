package com.odde.doughnut.services.ai.tools;

import com.theokanning.openai.assistants.assistant.FunctionTool;
import com.theokanning.openai.assistants.assistant.Tool;
import com.theokanning.openai.completion.chat.ChatTool;

public record AiTool(String name, String description, Class<?> parameterClass) {
  public Tool getTool() {
    @SuppressWarnings("unchecked")
    Class<Object> castParameterClass = (Class<Object>) parameterClass;
    return new FunctionTool(
        FunctionDefinition.builder()
            .name(name)
            .description(description)
            .strict(Boolean.TRUE)
            .parametersDefinitionByClass(castParameterClass)
            .build());
  }

  /** Get tool definition for Chat Completion API */
  public ChatTool getChatTool() {
    return new ChatTool(getFunctionDefinition());
  }

  /**
   * we are copying and modifying the FunctionDefinition class to add the strict field to avoid
   * breaking changes this won't be needed after our pull request is merged
   * https://github.com/Lambdua/openai4j/pull/74
   */
  @SuppressWarnings("unchecked")
  public com.theokanning.openai.function.FunctionDefinition getFunctionDefinition() {
    Class<Object> castParameterClass = (Class<Object>) parameterClass;
    return com.theokanning.openai.function.FunctionDefinition.builder()
        .name(name)
        .description(description)
        .parametersDefinitionByClass(castParameterClass)
        .build();
  }
}
