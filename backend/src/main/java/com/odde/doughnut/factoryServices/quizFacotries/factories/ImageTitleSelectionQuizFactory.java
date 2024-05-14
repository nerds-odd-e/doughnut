package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionImageTitle;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class ImageTitleSelectionQuizFactory extends ClozeTitleSelectionQuizFactory {
  public ImageTitleSelectionQuizFactory(Note note) {
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
    QuizQuestionImageTitle quizQuestionImageTitle = new QuizQuestionImageTitle();
    quizQuestionImageTitle.setNote(note);
    return quizQuestionImageTitle;
  }
}
