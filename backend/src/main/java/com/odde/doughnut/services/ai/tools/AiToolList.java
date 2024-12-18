package com.odde.doughnut.services.ai.tools;

import com.theokanning.openai.function.FunctionDefinition;
import java.util.*;
import lombok.Getter;

public class AiToolList {
  @Getter private final Map<String, FunctionDefinition> functions = new HashMap<>();
  @Getter private String messageBody;

  public AiToolList(String message, List<FunctionDefinition> functions) {
    this.messageBody = message;
    functions.forEach(f -> this.functions.put(f.getName(), f));
  }

  public String getFirstFunctionName() {
    return functions.keySet().iterator().next();
  }
}
