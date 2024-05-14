package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionPictureTitle;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class PictureTitleSelectionQuizFactory extends ClozeTitleSelectionQuizFactory {
  public PictureTitleSelectionQuizFactory(Note note) {
    super(note);
  }

  @Override
  public void validateBasicPossibility() throws QuizQuestionNotPossibleException {
    if (note.getImageWithMask().isEmpty()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public QuizQuestionWithNoteChoices buildQuizQuestionObj(QuizQuestionServant servant) {
    QuizQuestionPictureTitle quizQuestionPictureTitle = new QuizQuestionPictureTitle();
    quizQuestionPictureTitle.setNote(note);
    return quizQuestionPictureTitle;
  }
}
