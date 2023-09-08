package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.services.ai.OpenAIConfig;

public interface QuestionRawJsonFactory {
  void generateRawJsonQuestion(QuizQuestionEntity quizQuestion, OpenAIConfig config)
      throws QuizQuestionNotPossibleException;
}
