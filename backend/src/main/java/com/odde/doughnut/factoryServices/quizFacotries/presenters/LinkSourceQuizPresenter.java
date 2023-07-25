package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.QuizQuestionEntity;

public class LinkSourceQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Link link;

  public LinkSourceQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getThing().getLink();
  }

  @Override
  public String mainTopic() {
    return link.getTargetNote().getTitle();
  }

  @Override
  public String stem() {
    return "Which one <em>is immediately " + link.getLinkTypeLabel() + "</em>:";
  }
}
