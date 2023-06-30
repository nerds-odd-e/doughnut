package com.odde.doughnut.entities.json;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class AiCompetionRequest {
  public String prompt;
  public String incompleteAssistantMessage;
}
