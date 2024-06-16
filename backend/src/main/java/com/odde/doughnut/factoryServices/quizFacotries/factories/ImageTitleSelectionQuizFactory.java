package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class ImageTitleSelectionQuizFactory extends ClozeTitleSelectionQuizFactory {
  public ImageTitleSelectionQuizFactory(Note note, QuizQuestionServant servant) {
    super(note, servant);
  }

  @Override
  public QuizQuestion buildValidQuizQuestion() throws QuizQuestionNotPossibleException {
    QuizQuestion quizQuestion = super.buildValidQuizQuestion();
    quizQuestion.getQuizQuestion1().setImageWithMask(note.getImageWithMask());
    return quizQuestion;
  }

  @Override
  public void validateBasicPossibility() throws QuizQuestionNotPossibleException {
    if (note.getImageWithMask() == null) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public String getStem() {
    return note.getClozeDescription().clozeDetails();
  }
}
