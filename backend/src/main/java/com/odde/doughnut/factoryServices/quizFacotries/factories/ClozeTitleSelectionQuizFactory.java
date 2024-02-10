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
  protected QuizQuestionServant servant;

  public ClozeTitleSelectionQuizFactory(Note note, QuizQuestionServant servant) {
    this.note = note;
    this.servant = servant;
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
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    if (note.isDetailsBlankHtml()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public QuizQuestionEntity buildQuizQuestion() {
    return new QuizQuestionClozeSelection();
  }
}
