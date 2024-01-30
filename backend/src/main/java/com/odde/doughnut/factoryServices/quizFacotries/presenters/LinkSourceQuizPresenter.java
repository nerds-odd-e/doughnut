package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.Thing;

public class LinkSourceQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Thing link;

  public LinkSourceQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getThing();
  }

  @Override
  public String mainTopic() {
    return link.getTargetNote().getTopicConstructor();
  }

  @Override
  public String stem() {
    return "Which one <em>is immediately " + link.getLinkTypeLabel() + "</em>:";
  }
}
