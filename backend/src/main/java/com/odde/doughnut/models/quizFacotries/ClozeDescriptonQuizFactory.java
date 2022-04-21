package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import java.util.List;

public abstract class ClozeDescriptonQuizFactory implements QuizQuestionFactory {
  protected final ReviewPoint reviewPoint;
  protected final Note answerNote;

  public ClozeDescriptonQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.reviewPoint = reviewPoint;
    this.answerNote = this.reviewPoint.getNote();
  }

  @Override
  public List<Note> knownRightAnswers() {
    return List.of(answerNote);
  }
}
