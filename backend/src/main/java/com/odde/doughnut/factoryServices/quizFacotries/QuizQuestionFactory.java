package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;

public abstract class QuizQuestionFactory {
  public QuizQuestion buildValidQuizQuestion(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException {
    return null;
  }
}
