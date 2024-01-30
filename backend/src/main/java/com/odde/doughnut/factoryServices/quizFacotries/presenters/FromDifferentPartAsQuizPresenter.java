package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.*;

public class FromDifferentPartAsQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Thing link;
  private Thing categoryLink;

  public FromDifferentPartAsQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getThing();
    this.categoryLink = quizQuestion.getCategoryLink();
  }

  @Override
  public String stem() {
    return "<p>Which one <mark>is "
        + link.getLinkTypeLabel()
        + "</mark> a <em>DIFFERENT</em> "
        + categoryLink.getLinkType().nameOfSource
        + " of <mark>"
        + categoryLink.getTargetNote().getTopicConstructor()
        + "</mark> than:";
  }

  @Override
  public String mainTopic() {
    return link.getSourceNote().getTopicConstructor();
  }
}
