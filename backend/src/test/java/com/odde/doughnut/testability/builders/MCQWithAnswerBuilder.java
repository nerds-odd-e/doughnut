package com.odde.doughnut.testability.builders;

import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import java.util.List;

public class MCQWithAnswerBuilder {
  MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();

  public MCQWithAnswer please() {
    MultipleChoicesQuestion mcq = mcqWithAnswer.getF0__multipleChoicesQuestion();
    if (mcq.getF0__stem() == null) {
      mcq.setF0__stem("a default question stem");
    }
    if (mcq.getF1__choices() == null) {
      mcq.setF1__choices(List.of("choice1", "choice2", "choice3"));
    }
    return mcqWithAnswer;
  }

  public MCQWithAnswerBuilder stem(String stem) {
    mcqWithAnswer.getF0__multipleChoicesQuestion().setF0__stem(stem);
    return this;
  }

  public MCQWithAnswerBuilder choices(String... choices) {
    mcqWithAnswer.getF0__multipleChoicesQuestion().setF1__choices(List.of(choices));
    return this;
  }

  public MCQWithAnswerBuilder correctChoiceIndex(int i) {
    mcqWithAnswer.setF1__correctChoiceIndex(i);
    return this;
  }

  public MCQWithAnswerBuilder strictChoiceOrder(boolean b) {
    mcqWithAnswer.setF2__strictChoiceOrder(b);
    return this;
  }
}
