package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionPictureSelection;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class PictureSelectionQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {
  private final Note answerNote;

  public PictureSelectionQuizFactory(Note note) {
    this.answerNote = note;
  }

  @Override
  public List<Note> generateFillingOptions(QuizQuestionServant servant) {
    return servant.chooseFromCohort(answerNote, n -> n.getPictureWithMask().isPresent());
  }

  @Override
  public Note generateAnswer(QuizQuestionServant servant) {
    return answerNote;
  }

  @Override
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    if (answerNote.getPictureWithMask().isEmpty()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant) {
    return new QuizQuestionPictureSelection();
  }
}
