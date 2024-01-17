package com.odde.doughnut.controllers.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class AiCompletionResponse {
  String threadId;
  String runId;
  String moreCompleteContent;
  ClarifyingQuestionRequiredAction clarifyingQuestionRequiredAction;

  public String getMoreCompleteContent() {
    if (moreCompleteContent == null) {
      return "";
    }
    return moreCompleteContent;
  }
}
