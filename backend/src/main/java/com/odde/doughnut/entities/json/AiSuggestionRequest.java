package com.odde.doughnut.entities.json;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestionRequest {
  public String prompt;
  public String incompleteAssistantMessage;
}
