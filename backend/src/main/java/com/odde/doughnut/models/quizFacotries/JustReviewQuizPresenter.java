package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Answer;
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
  public boolean isAnswerCorrect(Answer answer) {
    return "yes".equals(answer.getSpellingAnswer());
  }
}
