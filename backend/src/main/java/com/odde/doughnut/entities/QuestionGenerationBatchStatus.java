package com.odde.doughnut.entities;

import java.util.EnumSet;
import java.util.Set;

public enum QuestionGenerationBatchStatus {
  PLANNED,
  SUBMITTED,
  COMPLETED,
  FAILED,
  EXPIRED;

  private static final Set<QuestionGenerationBatchStatus> TERMINAL_STATUSES =
      EnumSet.of(COMPLETED, FAILED, EXPIRED);

  private static final Set<QuestionGenerationBatchStatus> OPENAI_FAILURE_RETRY_STATUSES =
      EnumSet.of(FAILED, EXPIRED);

  public boolean isTerminal() {
    return TERMINAL_STATUSES.contains(this);
  }

  public static Set<QuestionGenerationBatchStatus> openAiFailureRetryStatuses() {
    return OPENAI_FAILURE_RETRY_STATUSES;
  }
}
