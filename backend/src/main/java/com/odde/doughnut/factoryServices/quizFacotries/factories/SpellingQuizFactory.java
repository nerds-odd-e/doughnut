package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class SpellingQuizFactory implements QuizQuestionFactory {

  protected final Note answerNote;
  protected QuizQuestionServant servant;

  public SpellingQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    this.servant = servant;
    this.answerNote = reviewPoint.getNote();
  }

  @Override
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    if (!needSpellingQuiz()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  private boolean needSpellingQuiz() {
    if (answerNote.isDescriptionBlankHtml()) {
      return false;
    }
    ReviewSetting reviewSetting = answerNote.getMasterReviewSetting();
    return reviewSetting != null && reviewSetting.getRememberSpelling();
  }
}
