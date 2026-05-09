package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription("refine the question")
public class MCQWithAnswerForRefinement extends MCQWithAnswer {
  public MCQWithAnswerForRefinement() {
    super();
  }

  public MCQWithAnswerForRefinement(
      MultipleChoicesQuestion multipleChoicesQuestion,
      int correctChoiceIndex,
      boolean choicesMayBeShuffled) {
    super(multipleChoicesQuestion, correctChoiceIndex, choicesMayBeShuffled, null, null);
  }
}
