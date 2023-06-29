package com.odde.doughnut.models.quizFacotries;

public interface QuestionRawJsonFactory {
  String generateRawJsonQuestion() throws QuizQuestionNotPossibleException;
}
