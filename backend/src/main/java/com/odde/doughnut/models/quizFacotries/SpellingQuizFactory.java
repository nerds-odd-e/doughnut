package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import org.apache.logging.log4j.util.Strings;

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
  public boolean isValidQuestion() {
    Note note = reviewPoint.getNote();
    if (!Strings.isEmpty(note.getTextContent().getDescription())) {
      ReviewSetting reviewSetting = note.getMasterReviewSetting();
      return reviewSetting != null && reviewSetting.getRememberSpelling();
    }
    return false;
  }
}
