package com.odde.doughnut.controllers.json;

import com.odde.doughnut.services.ai.ClarifyingQuestion;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class AiCompletionResponse {
  String threadId;
  String finishReason;
  String moreCompleteContent;
  ClarifyingQuestion clarifyingQuestion;
  List<ClarifyingQuestionAndAnswer> clarifyingHistory = new ArrayList<>();

  public void addClarifyingHistory(ClarifyingQuestionAndAnswer clarifyingQuestionAndAnswer) {
    clarifyingHistory.add(clarifyingQuestionAndAnswer);
  }
}
