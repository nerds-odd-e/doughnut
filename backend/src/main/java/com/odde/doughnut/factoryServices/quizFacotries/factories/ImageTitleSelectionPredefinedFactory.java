package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionServant;

public class ImageTitleSelectionPredefinedFactory extends ClozeTitleSelectionPredefinedFactory {
  public ImageTitleSelectionPredefinedFactory(Note note, PredefinedQuestionServant servant) {
    super(note, servant);
  }

  @Override
  public PredefinedQuestion buildValidPredefinedQuestion()
      throws PredefinedQuestionNotPossibleException {
    PredefinedQuestion predefinedQuestion = super.buildValidPredefinedQuestion();
    predefinedQuestion.getBareQuestion().setImageWithMask(note.getImageWithMask());
    return predefinedQuestion;
  }

  @Override
  public void validateBasicPossibility() throws PredefinedQuestionNotPossibleException {
    if (note.getImageWithMask() == null) {
      throw new PredefinedQuestionNotPossibleException();
    }
  }

  @Override
  public String getStem() {
    return note.getClozeDescription().clozeDetails();
  }
}
