package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;

public abstract class QuizQuestionWithOptionsPresenter {
  public abstract String stem();

  public abstract String mainTopic();

  protected final QuizQuestionWithNoteChoices quizQuestion;

  public QuizQuestionWithOptionsPresenter(QuizQuestionWithNoteChoices quizQuestion) {
    this.quizQuestion = quizQuestion;
  }
}
