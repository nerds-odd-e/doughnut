package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;

public class PictureTitleSelectionQuizFactory extends ClozeTitleSelectionQuizFactory {
  public PictureTitleSelectionQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    super(reviewPoint, servant);
  }

  @Override
  public Note generateAnswer() throws QuizQuestionNotPossibleException {
    if (reviewPoint.getNote().getPictureWithMask().isEmpty()) {
      throw new QuizQuestionNotPossibleException();
    }
    return super.generateAnswer();
  }
}
