package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class ClozeTitleSelectionQuizFactory extends QuestionOptionsFactory {
  public ClozeTitleSelectionQuizFactory(Note note, QuizQuestionServant servant) {
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
  public void validateBasicPossibility() throws QuizQuestionNotPossibleException {
    if (note.isDetailsBlankHtml()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public String getStem() {
    return note.getClozeDescription().clozeDetails();
  }
}
