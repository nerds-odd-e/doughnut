package com.odde.doughnut.entities.json;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public final class AiCompletion {
  String moreCompleteContent;
  String finishReason;
}
