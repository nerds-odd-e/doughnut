package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class PictureSelectionQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {
  private final Note answerNote;
  private QuizQuestionServant servant;

  public PictureSelectionQuizFactory(Thing thing, QuizQuestionServant servant) {
    this.answerNote = thing.getNote();
    this.servant = servant;
  }

  @Override
  public List<Note> generateFillingOptions() {
    return servant
        .chooseFromCohort(
            answerNote, n -> !n.equals(answerNote) && n.getPictureWithMask().isPresent())
        .toList();
  }

  @Override
  public Note generateAnswer() {
    return answerNote;
  }

  @Override
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    if (answerNote.getPictureWithMask().isEmpty()) {
      throw new QuizQuestionNotPossibleException();
    }
  }
}
