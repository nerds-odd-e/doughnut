package com.odde.doughnut.testability.builders;

import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import java.util.List;

public class MCQWithAnswerBuilder {
  MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();

  public MCQWithAnswer please() {
    MultipleChoicesQuestion mcq = mcqWithAnswer.getQuestion();
    if (mcq.getQuestionStem() == null) {
      mcq.setQuestionStem("a default question stem");
    }
    if (mcq.getResponseChoices() == null) {
      mcq.setResponseChoices(List.of("choice1", "choice2", "choice3"));
    }
    return mcqWithAnswer;
  }

  public MCQWithAnswerBuilder stem(String stem) {
    mcqWithAnswer.getQuestion().setQuestionStem(stem);
    return this;
  }

  public MCQWithAnswerBuilder choices(String... choices) {
    mcqWithAnswer.getQuestion().setResponseChoices(List.of(choices));
    return this;
  }

  public MCQWithAnswerBuilder correctChoiceIndex(int i) {
    mcqWithAnswer.setSolutionChoiceIndex(i);
    return this;
  }

  public MCQWithAnswerBuilder strictChoiceOrder(boolean b) {
    mcqWithAnswer.setStrictChoiceOrder(b);
    return this;
  }
}
