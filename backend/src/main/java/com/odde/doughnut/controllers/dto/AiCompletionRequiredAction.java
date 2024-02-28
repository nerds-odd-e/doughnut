package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.services.ai.ClarifyingQuestion;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class AiCompletionRequiredAction {
  public String toolCallId;
  ClarifyingQuestion clarifyingQuestion;
  String contentToAppend;
}
