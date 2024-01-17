package com.odde.doughnut.controllers.json;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class AiCompletionResponse {
  String threadId;
  String runId;
  AiCompletionRequiredAction requiredAction;
}
