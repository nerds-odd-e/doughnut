package com.odde.doughnut.controllers.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class QuestionSuggestionCreationParams {
  public String comment;
  public boolean isPositiveFeedback;
}
