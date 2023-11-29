package com.odde.doughnut.controllers.json;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class AiCompletion {
  String moreCompleteContent;
  String finishReason;
  String question;
  //  List<ClarifyingQuestionAndAnswer> clarifyingHistory = new ArrayList<>();
}
