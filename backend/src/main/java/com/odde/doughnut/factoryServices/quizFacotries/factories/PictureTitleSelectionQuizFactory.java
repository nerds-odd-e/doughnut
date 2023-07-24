package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

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
