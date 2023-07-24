package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class PictureTitleSelectionQuizFactory extends ClozeTitleSelectionQuizFactory {
  public PictureTitleSelectionQuizFactory(Thing thing, QuizQuestionServant servant) {
    super(thing, servant);
  }

  @Override
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    if (thing.getNote().getPictureWithMask().isEmpty()) {
      throw new QuizQuestionNotPossibleException();
    }
  }
}
