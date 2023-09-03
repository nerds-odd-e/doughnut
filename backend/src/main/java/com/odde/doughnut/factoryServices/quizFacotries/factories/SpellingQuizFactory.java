package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class SpellingQuizFactory implements QuizQuestionFactory {

  protected final Note answerNote;
  protected QuizQuestionServant servant;

  public SpellingQuizFactory(Thing thing, QuizQuestionServant servant) {
    this.servant = servant;
    this.answerNote = thing.getNote();
  }

  @Override
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    if (!needSpellingQuiz()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  private boolean needSpellingQuiz() {
    if (answerNote.isDetailsBlankHtml()) {
      return false;
    }
    ReviewSetting reviewSetting = answerNote.getMasterReviewSetting();
    return reviewSetting != null && reviewSetting.getRememberSpelling();
  }
}
