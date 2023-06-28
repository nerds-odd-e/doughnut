package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.json.QuizQuestionViewedByUser;
import java.util.List;

public class PictureSelectionQuizPresenter implements QuizQuestionPresenter {

  private ReviewPoint reviewPoint;

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
  public QuizQuestionViewedByUser.OptionCreator optionCreator() {
    return new QuizQuestionViewedByUser.PictureOptionCreator();
  }

  @Override
  public List<Note> knownRightAnswers() {
    return List.of(reviewPoint.getNote());
  }
}
