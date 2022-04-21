package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import java.util.List;

public class PictureSelectionQuizFactory implements QuizQuestionFactory, QuestionOptionsFactory {
  private final Note answerNote;
  private QuizQuestionServant servant;

  public PictureSelectionQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.answerNote = reviewPoint.getNote();
    this.servant = servant;
  }

  @Override
  public List<Note> generateFillingOptions() {
    return servant.chooseFromCohort(
        answerNote, n -> !n.equals(answerNote) && n.getPictureWithMask().isPresent());
  }

  @Override
  public Note generateAnswerNote() {
    return answerNote;
  }

  @Override
  public boolean isValidQuestion() {
    return answerNote.getPictureWithMask().isPresent();
  }

  @Override
  public int minimumOptionCount() {
    return 2;
  }

  @Override
  public List<Note> knownRightAnswers() {
    return List.of(answerNote);
  }
}
