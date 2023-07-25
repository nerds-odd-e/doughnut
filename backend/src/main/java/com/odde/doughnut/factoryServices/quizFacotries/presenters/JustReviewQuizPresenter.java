package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;

public class JustReviewQuizPresenter implements QuizQuestionPresenter {
  public JustReviewQuizPresenter(QuizQuestionEntity quizQuestion) {}

  @Override
  public String stem() {
    return null;
  }

  @Override
  public String mainTopic() {
    return null;
  }
}
