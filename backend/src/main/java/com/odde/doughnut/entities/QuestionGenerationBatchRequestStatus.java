package com.odde.doughnut.entities;

import java.util.EnumSet;
import java.util.Set;

public enum QuestionGenerationBatchRequestStatus {
  PENDING,
  OUTPUT_READY,
  FAILED,
  IMPORTED;

  private static final Set<QuestionGenerationBatchRequestStatus> TERMINAL_STATUSES =
      EnumSet.of(OUTPUT_READY, FAILED, IMPORTED);

  public boolean isTerminal() {
    return TERMINAL_STATUSES.contains(this);
  }
}
