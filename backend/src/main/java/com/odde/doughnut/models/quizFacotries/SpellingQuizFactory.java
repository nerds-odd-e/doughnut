package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;

public class SpellingQuizFactory implements QuizQuestionFactory {

  protected final ReviewPoint reviewPoint;
  protected final Note answerNote;
  protected QuizQuestionServant servant;

  public SpellingQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.reviewPoint = reviewPoint;
    this.servant = servant;
    this.answerNote = this.reviewPoint.getNote();
  }

  @Override
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    if (!needSpellingQuiz()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  private boolean needSpellingQuiz() {
    Note note = reviewPoint.getNote();
    if (note.isDescriptionBlankHtml()) {
      return false;
    }
    ReviewSetting reviewSetting = note.getMasterReviewSetting();
    return reviewSetting != null && reviewSetting.getRememberSpelling();
  }
}
