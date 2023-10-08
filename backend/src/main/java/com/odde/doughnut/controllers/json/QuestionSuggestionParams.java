package com.odde.doughnut.controllers.json;

import com.odde.doughnut.services.ai.MCQWithAnswer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class QuestionSuggestionParams extends QuestionSuggestionCreationParams {
  public MCQWithAnswer preservedQuestion;
}
