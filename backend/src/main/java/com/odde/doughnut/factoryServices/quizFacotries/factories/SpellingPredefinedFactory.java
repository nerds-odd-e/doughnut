package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionNotPossibleException;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;

public class SpellingPredefinedFactory extends PredefinedQuestionFactory {

  protected final Note answerNote;

  public SpellingPredefinedFactory(Note note) {
    this.answerNote = note;
  }

  @Override
  public PredefinedQuestion buildValidPredefinedQuestion()
      throws PredefinedQuestionNotPossibleException {
    if (!needSpellingQuiz()) {
      throw new PredefinedQuestionNotPossibleException();
    }
    return buildSpellingQuestion();
  }

  private boolean needSpellingQuiz() {
    if (answerNote.isDetailsBlankHtml()) {
      return false;
    }
    ReviewSetting reviewSetting = answerNote.getReviewSetting();
    return reviewSetting != null && reviewSetting.getRememberSpelling();
  }

  private String getStem() {
    return answerNote.getClozeDescription().clozeDetails();
  }

  public PredefinedQuestion buildSpellingQuestion() {
    PredefinedQuestion predefinedQuestionSpelling = new PredefinedQuestion();
    predefinedQuestionSpelling.setNote(answerNote);
    predefinedQuestionSpelling.setApproved(true);
    predefinedQuestionSpelling.getQuizQuestion1().setCheckSpell(true);
    MultipleChoicesQuestion mcq = new MultipleChoicesQuestion();
    mcq.setStem(getStem());
    predefinedQuestionSpelling.getQuizQuestion1().setMultipleChoicesQuestion(mcq);
    return predefinedQuestionSpelling;
  }
}
