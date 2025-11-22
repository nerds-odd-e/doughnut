package com.odde.doughnut.services.ai.tools;

public record AiTool(String name, String description, Class<?> parameterClass) {
  /** Get parameter class for use with builder.addTool(Class) and responseFormat(Class) */
  @SuppressWarnings("unchecked")
  public Class<Object> getParameterClass() {
    return (Class<Object>) parameterClass;
  }
}
