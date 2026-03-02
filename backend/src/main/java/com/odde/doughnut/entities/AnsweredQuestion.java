package com.odde.doughnut.entities;

import jakarta.validation.constraints.NotNull;

public class AnsweredQuestion {
  public Note note;
  @NotNull public RecallPrompt recallPrompt;
  @NotNull public Answer answer;
  public Integer memoryTrackerId;
  public Boolean thresholdExceeded;
}
