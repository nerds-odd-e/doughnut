package com.odde.doughnut.controllers.json;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class AiCompletionAnswerClarifyingQuestionParams extends AiCompletionParams {
  private String threadId;
  private String toolCallId;
  private String answer;
}
