package com.odde.doughnut.entities;

import jakarta.validation.constraints.NotNull;

public class AnsweredQuestion {
  public ReviewPoint reviewPoint;
  @NotNull public PredefinedQuestion predefinedQuestion;
  @NotNull public Answer answer;
}
