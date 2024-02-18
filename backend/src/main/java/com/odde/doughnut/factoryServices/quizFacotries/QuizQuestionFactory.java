package com.odde.doughnut.factoryServices.quizFacotries;

import com.odde.doughnut.entities.QuizQuestionEntity;

public interface QuizQuestionFactory {

  QuizQuestionEntity buildQuizQuestion(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException;
}
