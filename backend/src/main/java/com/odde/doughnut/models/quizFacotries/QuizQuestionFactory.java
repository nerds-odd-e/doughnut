package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;

public interface QuizQuestionFactory {
  default boolean isValidQuestion() {
    return true;
  }

  default void fillQuizQuestion(QuizQuestion quizQuestion) {}
}
