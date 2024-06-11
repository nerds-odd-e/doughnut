package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.services.ai.MCQWithAnswer;

public abstract class QuizQuestionFactory {
  public QuizQuestion buildValidQuizQuestion(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException {
    return null;
  }

  public QuizQuestion refineQuestion(MCQWithAnswer mcqWithAnswer)
      throws QuizQuestionNotPossibleException {
    return null;
  }
}
