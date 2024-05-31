package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.QuizQuestionEntity;

public abstract class QuizQuestionFactory {
  public String getStem() {
    return "";
  }

  public QuizQuestionEntity buildQuizQuestion(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException {
    return null;
  }
}
