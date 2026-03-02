package com.odde.doughnut.entities;

import jakarta.validation.constraints.NotNull;

public class AnsweredQuestion {
  @NotNull public RecallPrompt recallPrompt;
  public Boolean thresholdExceeded;
}
