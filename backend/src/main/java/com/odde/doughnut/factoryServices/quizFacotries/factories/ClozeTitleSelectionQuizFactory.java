package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class ClozeTitleSelectionQuizFactory implements QuestionOptionsFactory, QuizQuestionFactory {

  protected final Thing thing;
  protected final Note answerNote;
  protected QuizQuestionServant servant;

  public ClozeTitleSelectionQuizFactory(Thing thing, QuizQuestionServant servant) {
    this.thing = thing;
    this.servant = servant;
    this.answerNote = this.thing.getNote();
  }

  @Override
  public Note generateAnswer() {
    return answerNote;
  }

  @Override
  public List<Note> generateFillingOptions() {
    return servant.chooseFromCohort(answerNote, n -> !n.equals(answerNote));
  }

  @Override
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    if (thing.isDescriptionBlankHtml()) {
      throw new QuizQuestionNotPossibleException();
    }
  }
}
