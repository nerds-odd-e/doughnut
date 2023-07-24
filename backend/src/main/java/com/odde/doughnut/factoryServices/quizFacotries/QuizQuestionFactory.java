package com.odde.doughnut.factoryServices.quizFacotries;

public interface QuizQuestionFactory {
  default void validatePossibility() throws QuizQuestionNotPossibleException {}
}
