package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;

public abstract class ClozeDescriptonQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Note note;

  public ClozeDescriptonQuizPresenter(QuizQuestionWithNoteChoices quizQuestion) {
    super(quizQuestion);
    this.note = quizQuestion.getNote();
  }

  @Override
  public String mainTopic() {
    return "";
  }

  @Override
  public String stem() {
    return note.getClozeDescription().clozeDetails();
  }
}
