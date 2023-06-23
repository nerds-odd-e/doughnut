package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewPoint;

public class AiQuestionFactory implements QuizQuestionFactory {
  public AiQuestionFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {}

  @Override
  public void fillQuizQuestion(QuizQuestion quizQuestion) {
    quizQuestion.setRawJsonQuestion("what is the AI's answer?");
  }
}
