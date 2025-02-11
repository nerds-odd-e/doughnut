package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;

public class SpellingPredefinedFactory {

  protected final Note answerNote;

  public SpellingPredefinedFactory(Note note) {
    this.answerNote = note;
  }

  private String getStem() {
    return answerNote.getClozeDescription().clozeDetails();
  }

  public PredefinedQuestion buildSpellingQuestion() {
    PredefinedQuestion predefinedQuestionSpelling = new PredefinedQuestion();
    predefinedQuestionSpelling.setNote(answerNote);
    predefinedQuestionSpelling.setApproved(true);
    predefinedQuestionSpelling.getBareQuestion().setCheckSpell(true);
    MultipleChoicesQuestion mcq = new MultipleChoicesQuestion();
    mcq.setStem(getStem());
    predefinedQuestionSpelling.getBareQuestion().setMultipleChoicesQuestion(mcq);
    return predefinedQuestionSpelling;
  }
}
