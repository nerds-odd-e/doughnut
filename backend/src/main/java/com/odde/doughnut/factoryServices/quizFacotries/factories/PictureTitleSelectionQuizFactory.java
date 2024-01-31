package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class PictureTitleSelectionQuizFactory extends ClozeTitleSelectionQuizFactory {
  public PictureTitleSelectionQuizFactory(Note note, QuizQuestionServant servant) {
    super(note, servant);
  }

  @Override
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    if (note.getPictureWithMask().isEmpty()) {
      throw new QuizQuestionNotPossibleException();
    }
  }
}
