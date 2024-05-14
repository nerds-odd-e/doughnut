package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionImageSelection;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.List;

public class ImageSelectionQuizFactory extends QuestionOptionsFactory {
  private final Note answerNote;

  public ImageSelectionQuizFactory(Note note) {
    this.answerNote = note;
  }

  @Override
  public List<Note> generateFillingOptions(QuizQuestionServant servant) {
    return servant.chooseFromCohort(answerNote, n -> n.getImageWithMask().isPresent());
  }

  @Override
  public Note generateAnswer(QuizQuestionServant servant) {
    return answerNote;
  }

  @Override
  public void validateBasicPossibility() throws QuizQuestionNotPossibleException {
    if (answerNote.getImageWithMask().isEmpty()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public QuizQuestionImageSelection buildQuizQuestionObj(QuizQuestionServant servant) {

    QuizQuestionImageSelection quizQuestion = new QuizQuestionImageSelection();
    quizQuestion.setNote(answerNote);
    return quizQuestion;
  }
}
