package com.odde.doughnut.controllers.json;

import com.odde.doughnut.services.ai.ClarifyingQuestion;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@Data
@NoArgsConstructor
public final class AiCompletionRequiredAction {
  public String toolCallId;
  @Nullable ClarifyingQuestion clarifyingQuestion;
  @Nullable String contentToAppend;
}
