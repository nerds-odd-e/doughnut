package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;

public interface QuestionRawJsonFactory {
  void generateRawJsonQuestion(QuizQuestionEntity quizQuestion, String model, Double temperature)
      throws QuizQuestionNotPossibleException;
}
