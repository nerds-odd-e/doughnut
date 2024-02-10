package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.QuizQuestionEntity;

public interface QuizQuestionFactory {
  default void validatePossibility() throws QuizQuestionNotPossibleException {}

  QuizQuestionEntity buildQuizQuestion();
}
