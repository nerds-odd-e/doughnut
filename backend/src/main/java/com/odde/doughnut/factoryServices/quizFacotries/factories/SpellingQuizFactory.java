package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestion;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import com.odde.doughnut.services.ai.MCQWithAnswer;

public class SpellingQuizFactory extends QuizQuestionFactory {

  protected final Note answerNote;

  public SpellingQuizFactory(Note note) {
    this.answerNote = note;
  }

  @Override
  public QuizQuestion buildQuizQuestion(QuizQuestionServant servant)
      throws QuizQuestionNotPossibleException {
    if (!needSpellingQuiz()) {
      throw new QuizQuestionNotPossibleException();
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

  @Override
  public String getStem() {
    return answerNote.getClozeDescription().clozeDetails();
  }

  public QuizQuestion buildSpellingQuestion() {
    QuizQuestion quizQuestionSpelling = new QuizQuestion();
    quizQuestionSpelling.setNote(answerNote);
    quizQuestionSpelling.setCheckSpell(true);
    MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();
    mcqWithAnswer.stem = getStem();
    quizQuestionSpelling.setMcqWithAnswer(mcqWithAnswer);
    return quizQuestionSpelling;
  }
}
