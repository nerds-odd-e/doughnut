package com.odde.doughnut.controllers.json;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
@Data
public class AiCompletionAnswerClarifyingQuestionParams extends AiCompletionParams {
  private String threadId;
  private String runId;
  private String toolCallId;
  private String answer;
}
