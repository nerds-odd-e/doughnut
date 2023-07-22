package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestionEntity;

public interface QuestionRawJsonFactory {
  void generateRawJsonQuestion(QuizQuestionEntity quizQuestion)
      throws QuizQuestionNotPossibleException;
}
