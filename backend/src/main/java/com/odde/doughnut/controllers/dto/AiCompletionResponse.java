package com.odde.doughnut.controllers.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class AiCompletionResponse {
  String threadId;
  String runId;
  AiCompletionRequiredAction requiredAction;
  String lastMessage;
}
