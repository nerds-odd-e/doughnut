package com.odde.doughnut.services.ai.tools;

import com.theokanning.openai.assistants.assistant.FunctionTool;
import com.theokanning.openai.assistants.assistant.Tool;

public record AiTool(String name, String description, Class<?> parameterClass) {
  public Tool getTool() {
    @SuppressWarnings("unchecked")
    Class<Object> castParameterClass = (Class<Object>) parameterClass;
    return new FunctionTool(
        com.theokanning.openai.function.FunctionDefinition.builder()
            .name(name)
            .description(description)
            .strict(Boolean.TRUE)
            .parametersDefinitionByClass(castParameterClass)
            .build());
  }

  /** Get parameter class for use with builder.addTool(Class) */
  @SuppressWarnings("unchecked")
  public Class<Object> getParameterClass() {
    return (Class<Object>) parameterClass;
  }

  /**
   * Legacy method for backward compatibility - returns custom FunctionDefinition we are copying and
   * modifying the FunctionDefinition class to add the strict field to avoid breaking changes this
   * won't be needed after our pull request is merged https://github.com/Lambdua/openai4j/pull/74
   */
  @SuppressWarnings("unchecked")
  public FunctionDefinition getFunctionDefinition() {
    Class<Object> castParameterClass = (Class<Object>) parameterClass;
    return FunctionDefinition.builder()
        .name(name)
        .description(description)
        .strict(Boolean.TRUE)
        .parametersDefinitionByClass(castParameterClass)
        .build();
  }
}
