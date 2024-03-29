package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionPictureSelection;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class PictureSelectionQuizFactory extends QuestionOptionsFactory {
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
  public void validateBasicPossibility() throws QuizQuestionNotPossibleException {
    if (answerNote.getPictureWithMask().isEmpty()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public QuizQuestionPictureSelection buildQuizQuestionObj(QuizQuestionServant servant) {

    QuizQuestionPictureSelection quizQuestion = new QuizQuestionPictureSelection();
    quizQuestion.setNote(answerNote);
    return quizQuestion;
  }
}
