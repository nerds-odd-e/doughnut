package com.odde.doughnut.controllers.json;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class AiCompletion {
  String moreCompleteContent;
  String finishReason;
  String question;
  List<ClarifyingQuestionAndAnswer> clarifyingHistory = new ArrayList<>();

  public void addClarifyingHistory(ClarifyingQuestionAndAnswer clarifyingQuestionAndAnswer) {
    clarifyingHistory.add(clarifyingQuestionAndAnswer);
  }
}
