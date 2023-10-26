package com.odde.doughnut.controllers.json;

import com.odde.doughnut.services.ai.MCQWithAnswer;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class QuestionSuggestionParams {
  public String comment;
  public MCQWithAnswer preservedQuestion;
  public String preservedNoteContent;
  public boolean positiveFeedback;
  public String realCorrectAnswers;
}
