package com.odde.doughnut.controllers.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class AiCompletionRequiredAction {
  public String toolCallId;
  String contentToAppend;
}
