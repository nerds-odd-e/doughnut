package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.services.ai.MCQWithAnswer;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class QuestionSuggestionParams {
  public String comment;
  @NotNull public MCQWithAnswer preservedQuestion;
  public String preservedNoteContent;
  public boolean positiveFeedback;
  @NotNull public String realCorrectAnswers;
}
