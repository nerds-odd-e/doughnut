package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import org.apache.logging.log4j.util.Strings;

public class SpellingQuizFactory extends ClozeDescriptonQuizFactory {
  public SpellingQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    super(reviewPoint, servant);
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
