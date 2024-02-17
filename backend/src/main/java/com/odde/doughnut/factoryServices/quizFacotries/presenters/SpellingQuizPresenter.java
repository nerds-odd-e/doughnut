package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionSpelling;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionPresenter;

public class SpellingQuizPresenter implements QuizQuestionPresenter {
  Note note;

  public SpellingQuizPresenter(QuizQuestionSpelling quizQuestion) {
    note = quizQuestion.getNote();
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
