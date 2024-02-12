package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionClozeSelection;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class ClozeTitleSelectionQuizFactory implements QuestionOptionsFactory, QuizQuestionFactory {

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
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    if (note.isDetailsBlankHtml()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant) {
    return new QuizQuestionClozeSelection(note);
  }
}
