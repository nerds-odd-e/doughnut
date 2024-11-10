package com.odde.doughnut.services.ai.tools;

import com.theokanning.openai.assistants.assistant.FunctionTool;
import com.theokanning.openai.assistants.assistant.Tool;
import com.theokanning.openai.function.FunctionDefinition;

public record AiTool(String name, String description, Class<?> parameterClass) {
  public Tool getTool() {
    @SuppressWarnings("unchecked")
    Class<Object> castParameterClass = (Class<Object>) parameterClass;
    return new FunctionTool(
        FunctionDefinition.builder()
            .name(name)
            .description(description)
            .parametersDefinitionByClass(castParameterClass)
            .build());
  }
}
