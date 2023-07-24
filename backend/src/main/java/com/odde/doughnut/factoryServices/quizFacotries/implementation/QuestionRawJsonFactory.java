package com.odde.doughnut.factoryServices.quizFacotries.implementation;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;

public interface QuestionRawJsonFactory {
  void generateRawJsonQuestion(QuizQuestionEntity quizQuestion)
      throws QuizQuestionNotPossibleException;
}
