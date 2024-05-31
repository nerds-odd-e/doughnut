package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;

public abstract class QuizQuestionFactory {
  public String getStem() {
    return "";
  }

  public QuizQuestion buildQuizQuestion(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException {
    return null;
  }
}
