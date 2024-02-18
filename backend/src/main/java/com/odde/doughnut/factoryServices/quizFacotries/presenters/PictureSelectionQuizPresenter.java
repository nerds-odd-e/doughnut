package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;

public class PictureSelectionQuizPresenter extends QuizQuestionWithOptionsPresenter {

  private Note note;

  public PictureSelectionQuizPresenter(QuizQuestionWithNoteChoices quizQuestion) {
    super(quizQuestion);
    this.note = quizQuestion.getNote();
  }

  @Override
  public String mainTopic() {
    return note.getTopicConstructor();
  }

  @Override
  public String stem() {
    return "";
  }
}
