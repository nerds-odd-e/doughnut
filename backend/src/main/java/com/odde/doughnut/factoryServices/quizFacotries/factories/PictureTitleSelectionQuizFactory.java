package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionPictureTitle;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class PictureTitleSelectionQuizFactory extends ClozeTitleSelectionQuizFactory {
  public PictureTitleSelectionQuizFactory(Note note) {
    super(note);
  }

  @Override
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    if (note.getPictureWithMask().isEmpty()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant) {
    QuizQuestionPictureTitle quizQuestionPictureTitle = new QuizQuestionPictureTitle();
    quizQuestionPictureTitle.setNote(note);
    return quizQuestionPictureTitle;
  }
}
