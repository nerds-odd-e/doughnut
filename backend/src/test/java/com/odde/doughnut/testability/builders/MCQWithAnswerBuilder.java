package com.odde.doughnut.testability.builders;

import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import java.util.List;

public class MCQWithAnswerBuilder {
  MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();

  public MCQWithAnswer please() {
    MultipleChoicesQuestion mcq = mcqWithAnswer.multipleChoicesQuestion;
    if (mcq.stem == null) {
      mcq.stem = "a default question stem";
    }
    if (mcq.choices == null) {
      mcq.choices = List.of("choice1", "choice2", "choice3");
    }
    return mcqWithAnswer;
  }

  public MCQWithAnswerBuilder stem(String stem) {
    mcqWithAnswer.multipleChoicesQuestion.stem = stem;
    return this;
  }

  public MCQWithAnswerBuilder choices(String... choices) {
    mcqWithAnswer.multipleChoicesQuestion.choices = List.of(choices);
    return this;
  }

  public MCQWithAnswerBuilder correctChoiceIndex(int i) {
    mcqWithAnswer.correctChoiceIndex = i;
    return this;
  }
}
