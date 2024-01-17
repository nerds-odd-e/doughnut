package com.odde.doughnut.controllers.json;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@NoArgsConstructor
public final class AiCompletionResponse {
  String threadId;
  String runId;
  @Nullable AiCompletionRequiredAction requiredAction;
  String lastMessage;
}
