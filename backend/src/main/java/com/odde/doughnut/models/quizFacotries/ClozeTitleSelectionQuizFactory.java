package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import java.util.List;

public class ClozeTitleSelectionQuizFactory implements QuestionOptionsFactory, QuizQuestionFactory {

  protected final ReviewPoint reviewPoint;
  protected final Note answerNote;
  protected QuizQuestionServant servant;

  public ClozeTitleSelectionQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.reviewPoint = reviewPoint;
    this.servant = servant;
    this.answerNote = this.reviewPoint.getNote();
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
    if (reviewPoint.isDescriptionBlankHtml()) {
      throw new QuizQuestionNotPossibleException();
    }
  }
}
