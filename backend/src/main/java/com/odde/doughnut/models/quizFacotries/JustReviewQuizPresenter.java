package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestionEntity;

public class JustReviewQuizPresenter implements QuizQuestionPresenter {
  public JustReviewQuizPresenter(QuizQuestionEntity quizQuestion) {}

  @Override
  public String instruction() {
    return null;
  }

  @Override
  public String mainTopic() {
    return null;
  }

  @Override
  public boolean isAnswerCorrect(String spellingAnswer) {
    return "yes".equals(spellingAnswer);
  }
}
