package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionServant;
import java.util.List;

public class ClozeTitleSelectionPredefinedFactory extends QuestionOptionsFactory {
  public ClozeTitleSelectionPredefinedFactory(Note note, PredefinedQuestionServant servant) {
    super(note, servant);
  }

  @Override
  public Note generateAnswer() {
    return note;
  }

  @Override
  public List<Note> generateFillingOptions() {
    return servant.chooseFromCohort(note, n -> true);
  }

  @Override
  public void validateBasicPossibility() throws PredefinedQuestionNotPossibleException {
    if (note.isDetailsBlankHtml()) {
      throw new PredefinedQuestionNotPossibleException();
    }
  }

  @Override
  public String getStem() {
    return note.getClozeDescription().clozeDetails();
  }
}
