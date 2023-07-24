package com.odde.doughnut.factoryServices.quizFacotries.implementation;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;

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
}
