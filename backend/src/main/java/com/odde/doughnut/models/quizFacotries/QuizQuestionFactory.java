package com.odde.doughnut.models.quizFacotries;

public interface QuizQuestionFactory {
  default void validatePossibility() throws QuizQuestionNotPossibleException {}
}
