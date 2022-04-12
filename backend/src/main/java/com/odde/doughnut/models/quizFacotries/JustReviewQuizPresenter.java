package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestion;

public class JustReviewQuizPresenter implements QuizQuestionPresenter {

  public JustReviewQuizPresenter(QuizQuestion quizQuestion) {}

  @Override
  public String mainTopic() {
    return "";
  }

  @Override
  public String instruction() {
    return "";
  }
}
