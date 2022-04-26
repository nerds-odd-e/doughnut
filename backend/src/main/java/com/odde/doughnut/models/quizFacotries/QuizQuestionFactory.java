package com.odde.doughnut.models.quizFacotries;

public interface QuizQuestionFactory {
  default boolean isValidQuestion() {
    return true;
  }
}
