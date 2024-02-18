package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;

public class LinkSourceWithinSameLinkTypeQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Note link;

  public LinkSourceWithinSameLinkTypeQuizPresenter(QuizQuestionWithNoteChoices quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getNote();
  }

  @Override
  public String mainTopic() {
    return link.getTargetNote().getTopicConstructor();
  }

  @Override
  public String stem() {
    return "Which one <em>is immediately " + link.getLinkType().label + "</em>:";
  }
}
