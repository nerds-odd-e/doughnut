package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;

public interface QuizQuestionFactory {
  default void validatePossibility() throws QuizQuestionNotPossibleException {}

  default void fillQuizQuestion(QuizQuestion quizQuestion) {}
}
