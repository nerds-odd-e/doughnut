package com.odde.doughnut.controllers.json;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public final class AiCompletion {
  String moreCompleteContent;
  String finishReason;
  String question;
}
