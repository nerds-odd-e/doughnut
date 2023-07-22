package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;

public class FromDifferentPartAsQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Link link;
  private Link categoryLink;

  public FromDifferentPartAsQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getReviewPoint().getLink();
    this.categoryLink = quizQuestion.getCategoryLink();
  }

  @Override
  public String instruction() {
    return "<p>Which one <mark>is "
        + link.getLinkTypeLabel()
        + "</mark> a <em>DIFFERENT</em> "
        + categoryLink.getLinkType().nameOfSource
        + " of <mark>"
        + categoryLink.getTargetNote().getTitle()
        + "</mark> than:";
  }

  @Override
  public String mainTopic() {
    return link.getSourceNote().getTitle();
  }
}
