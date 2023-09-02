package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.QuizQuestionEntity;

public class WhichSpecHasInstanceQuizPresenter extends QuizQuestionWithOptionsPresenter {
  private Link instanceLink;
  private final Link link;

  public WhichSpecHasInstanceQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getThing().getLink();
    this.instanceLink = quizQuestion.getCategoryLink();
  }

  @Override
  public String mainTopic() {
    return null;
  }

  @Override
  public String stem() {
    return "<p>Which one is "
        + link.getLinkTypeLabel()
        + " <mark>"
        + link.getTargetNote().getTopic()
        + "</mark> <em>and</em> is "
        + instanceLink.getLinkTypeLabel()
        + " <mark>"
        + instanceLink.getTargetNote().getTopic()
        + "</mark>:";
  }
}
