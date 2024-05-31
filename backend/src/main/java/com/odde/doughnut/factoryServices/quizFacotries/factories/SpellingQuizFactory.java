package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionSpelling;
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
  public QuizQuestionEntity buildQuizQuestion(QuizQuestionServant servant)
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

  public QuizQuestionSpelling buildSpellingQuestion() {
    QuizQuestionSpelling quizQuestionSpelling = new QuizQuestionSpelling();
    quizQuestionSpelling.setNote(answerNote);
    MCQWithAnswer mcqWithAnswer = new MCQWithAnswer();
    mcqWithAnswer.stem = getStem();
    quizQuestionSpelling.setMcqWithAnswer(mcqWithAnswer);
    return quizQuestionSpelling;
  }
}
