package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.PictureWithMask;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewPoint;
import java.util.Optional;

public class PictureTitleSelectionQuizPresenter extends ClozeTitleSelectionQuizPresenter {
  ReviewPoint reviewPoint;

  public PictureTitleSelectionQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    reviewPoint = quizQuestion.getReviewPoint();
  }

  @Override
  public Optional<PictureWithMask> pictureWithMask() {
    return reviewPoint.getNote().getPictureWithMask();
  }
}
