package com.odde.doughnut.testability.builders;

import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import java.util.List;

public class MCQWithAnswerBuilder {
  MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();

  public MCQWithAnswer please() {
    MultipleChoicesQuestion mcq = mcqWithAnswer.getMultipleChoicesQuestion();
    if (mcq.getStem() == null) {
      mcq.setStem("a default question stem");
    }
    if (mcq.getChoices() == null) {
      mcq.setChoices(List.of("choice1", "choice2", "choice3"));
    }
    return mcqWithAnswer;
  }

  public MCQWithAnswerBuilder stem(String stem) {
    mcqWithAnswer.getMultipleChoicesQuestion().setStem(stem);
    return this;
  }

  public MCQWithAnswerBuilder choices(String... choices) {
    mcqWithAnswer.getMultipleChoicesQuestion().setChoices(List.of(choices));
    return this;
  }

  public MCQWithAnswerBuilder correctChoiceIndex(int i) {
    mcqWithAnswer.setCorrectChoiceIndex(i);
    return this;
  }
}
