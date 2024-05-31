package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class ClozeTitleSelectionQuizFactory extends QuestionOptionsFactory {

  protected final Note note;

  public ClozeTitleSelectionQuizFactory(Note note) {
    this.note = note;
  }

  @Override
  public Note generateAnswer(QuizQuestionServant servant) {
    return note;
  }

  @Override
  public List<Note> generateFillingOptions(QuizQuestionServant servant) {
    return servant.chooseFromCohort(note, n -> true);
  }

  @Override
  public void validateBasicPossibility() throws QuizQuestionNotPossibleException {
    if (note.isDetailsBlankHtml()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant) {
    QuizQuestionEntity quizQuestionClozeSelection = new QuizQuestionEntity();
    quizQuestionClozeSelection.setNote(note);
    return quizQuestionClozeSelection;
  }

  @Override
  public String getStem() {
    return note.getClozeDescription().clozeDetails();
  }
}
