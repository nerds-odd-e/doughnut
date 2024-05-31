package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class ImageTitleSelectionQuizFactory extends ClozeTitleSelectionQuizFactory {
  public ImageTitleSelectionQuizFactory(Note note) {
    super(note);
  }

  @Override
  public void validateBasicPossibility() throws QuizQuestionNotPossibleException {
    if (note.getImageWithMask() == null) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant) {
    QuizQuestionEntity quizQuestionImageTitle = new QuizQuestionEntity();
    quizQuestionImageTitle.setNote(note);
    quizQuestionImageTitle.setHasImage(true);
    return quizQuestionImageTitle;
  }

  @Override
  public String getStem() {
    return note.getClozeDescription().clozeDetails();
  }
}
