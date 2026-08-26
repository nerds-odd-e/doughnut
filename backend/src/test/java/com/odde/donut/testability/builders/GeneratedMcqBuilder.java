package com.odde.donut.testability.builders;

import com.odde.donut.services.ai.GeneratedMcq;
import java.util.List;

public class GeneratedMcqBuilder {
  GeneratedMcq generatedMcq = new GeneratedMcq();

  public GeneratedMcq please() {
    if (generatedMcq.getQuestionStem() == null) {
      generatedMcq.setQuestionStem("a default question stem");
    }
    if (generatedMcq.getCorrectAnswer() == null) {
      generatedMcq.setCorrectAnswer("correct answer");
    }
    if (generatedMcq.getDistractors() == null) {
      generatedMcq.setDistractors(List.of("distractor1", "distractor2", "distractor3"));
    }
    return generatedMcq;
  }

  public GeneratedMcqBuilder stem(String stem) {
    generatedMcq.setQuestionStem(stem);
    return this;
  }

  public GeneratedMcqBuilder correctAnswer(String correctAnswer) {
    generatedMcq.setCorrectAnswer(correctAnswer);
    return this;
  }

  public GeneratedMcqBuilder distractors(String... distractors) {
    generatedMcq.setDistractors(List.of(distractors));
    return this;
  }
}
