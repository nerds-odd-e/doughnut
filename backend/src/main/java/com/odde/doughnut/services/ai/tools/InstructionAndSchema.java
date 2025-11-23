package com.odde.doughnut.services.ai.tools;

import lombok.Getter;

public class InstructionAndSchema {
  @Getter private final Class<?> parameterClass;
  @Getter private String messageBody;

  public InstructionAndSchema(String message, Class<?> parameterClass) {
    this.messageBody = message;
    this.parameterClass = parameterClass;
  }
}
