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
  public Note generateAnswer() {
    return answerNote;
  }

  @Override
  public boolean isValidQuestion() throws QuizQuestionNotPossibleException {
    if(answerNote.getPictureWithMask().isEmpty()) {
      throw new QuizQuestionNotPossibleException();
    }
    return true;
  }
}
