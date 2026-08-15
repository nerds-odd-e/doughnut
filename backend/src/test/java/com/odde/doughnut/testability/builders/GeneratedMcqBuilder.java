package com.odde.doughnut.testability.builders;

import com.odde.doughnut.services.ai.GeneratedMcq;
import java.util.List;

public class GeneratedMcqBuilder {
  GeneratedMcq generatedMcq = new GeneratedMcq();

  public GeneratedMcq please() {
    if (generatedMcq.getQuestionStem() == null) {
      generatedMcq.setQuestionStem("a default question stem");
    }
    if (generatedMcq.getResponseChoices() == null) {
      generatedMcq.setResponseChoices(List.of("choice1", "choice2", "choice3"));
    }
    return generatedMcq;
  }

  public GeneratedMcqBuilder stem(String stem) {
    generatedMcq.setQuestionStem(stem);
    return this;
  }

  public GeneratedMcqBuilder choices(String... choices) {
    generatedMcq.setResponseChoices(List.of(choices));
    return this;
  }

  public GeneratedMcqBuilder correctAnswerIndex(int i) {
    generatedMcq.setCorrectAnswerIndex(i);
    return this;
  }

  public GeneratedMcqBuilder choicesMayBeShuffled(boolean b) {
    generatedMcq.setChoicesMayBeShuffled(b);
    return this;
  }
}
