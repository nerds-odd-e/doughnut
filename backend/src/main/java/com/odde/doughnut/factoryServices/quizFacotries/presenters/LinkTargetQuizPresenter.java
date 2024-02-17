package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;

public class LinkTargetQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Note link;
  protected final Note answerNote;

  public LinkTargetQuizPresenter(QuizQuestionWithNoteChoices quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getNote();
    this.answerNote = link.getTargetNote();
  }

  @Override
  public String mainTopic() {
    return "";
  }

  @Override
  public String stem() {
    return "<mark>"
        + link.getParent().getTopicConstructor()
        + "</mark> is "
        + link.getLinkType().label
        + ":";
  }
}
