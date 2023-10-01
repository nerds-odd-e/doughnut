package com.odde.doughnut.controllers.json;

import com.odde.doughnut.services.ai.MCQWithAnswer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class QuestionSuggestion {
  public String comment;
  public String suggestion;
  public MCQWithAnswer MCQWithAnswer;
}
