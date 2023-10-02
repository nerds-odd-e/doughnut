package com.odde.doughnut.testability.builders;

import com.odde.doughnut.services.ai.MCQWithAnswer;
import java.util.List;

public class MCQWithAnswerBuilder {
  MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();

  public MCQWithAnswer please() {
    if (mcqWithAnswer.stem == null) {
      mcqWithAnswer.stem = "a default question stem";
    }
    if (mcqWithAnswer.choices == null) {
      mcqWithAnswer.choices = List.of("choice1", "choice2", "choice3");
    }
    mcqWithAnswer.correctChoiceIndex = 2;
    return mcqWithAnswer;
  }

  public MCQWithAnswerBuilder stem(String stem) {
    mcqWithAnswer.stem = stem;
    return this;
  }

  public MCQWithAnswerBuilder choices(String... choices) {
    mcqWithAnswer.choices = List.of(choices);
    return this;
  }

  public MCQWithAnswerBuilder correctChoiceIndex(int i) {
    mcqWithAnswer.correctChoiceIndex = i;
    return this;
  }
}
