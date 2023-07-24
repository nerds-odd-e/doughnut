package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.ReviewPoint;

public class PictureTitleSelectionQuizFactory extends ClozeTitleSelectionQuizFactory {
  public PictureTitleSelectionQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    super(reviewPoint, servant);
  }

  @Override
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    if (thing.getNote().getPictureWithMask().isEmpty()) {
      throw new QuizQuestionNotPossibleException();
    }
  }
}
