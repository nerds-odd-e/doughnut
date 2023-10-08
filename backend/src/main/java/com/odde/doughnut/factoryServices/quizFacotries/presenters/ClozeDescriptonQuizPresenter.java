package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;

public abstract class ClozeDescriptonQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Note note;

  public ClozeDescriptonQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.note = quizQuestion.getThing().getNote();
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
