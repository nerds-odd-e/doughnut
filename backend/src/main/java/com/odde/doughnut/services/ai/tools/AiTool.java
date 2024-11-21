package com.odde.doughnut.services.ai.tools;

import com.theokanning.openai.assistants.assistant.FunctionTool;
import com.theokanning.openai.assistants.assistant.Tool;

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
}
