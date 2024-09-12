package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class ImageTitleSelectionQuizFactory extends ClozeTitleSelectionQuizFactory {
  public ImageTitleSelectionQuizFactory(Note note, QuizQuestionServant servant) {
    super(note, servant);
  }

  @Override
  public PredefinedQuestion buildValidQuizQuestion() throws QuizQuestionNotPossibleException {
    PredefinedQuestion predefinedQuestion = super.buildValidQuizQuestion();
    predefinedQuestion.getQuizQuestion1().setImageWithMask(note.getImageWithMask());
    return predefinedQuestion;
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
