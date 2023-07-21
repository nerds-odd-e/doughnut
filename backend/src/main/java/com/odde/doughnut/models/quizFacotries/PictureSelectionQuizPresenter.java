package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.QuizQuestion;

public class PictureSelectionQuizPresenter extends QuizQuestionWithOptionsPresenter {

  private ReviewPoint reviewPoint;

  @Override
  public boolean isAnswerCorrect(String spellingAnswer) {
    return reviewPoint.getNote().matchAnswer(spellingAnswer);
  }

  public PictureSelectionQuizPresenter(QuizQuestionEntity quizQuestion) {
    this.reviewPoint = quizQuestion.getReviewPoint();
  }

  @Override
  public String mainTopic() {
    return reviewPoint.getNote().getTitle();
  }

  @Override
  public String instruction() {
    return "";
  }

  @Override
  public QuizQuestion.OptionCreator optionCreator() {
    return new QuizQuestion.PictureOptionCreator();
  }
}
