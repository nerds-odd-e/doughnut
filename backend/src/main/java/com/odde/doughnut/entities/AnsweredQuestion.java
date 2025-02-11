package com.odde.doughnut.entities;

import jakarta.validation.constraints.NotNull;

public class AnsweredQuestion {
  public Note note;
  public PredefinedQuestion predefinedQuestion;
  @NotNull public Answer answer;
  public String answerDisplay;
  public Integer recallPromptId;
}
