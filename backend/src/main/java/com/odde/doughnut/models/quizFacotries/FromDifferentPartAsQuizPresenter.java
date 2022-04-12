package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.QuizQuestion;

public class FromDifferentPartAsQuizPresenter implements QuizQuestionPresenter {
  protected final Link link;
  private Link categoryLink;

  public FromDifferentPartAsQuizPresenter(QuizQuestion quizQuestion) {
    this.link = quizQuestion.getReviewPoint().getLink();
    this.categoryLink = quizQuestion.getCategoryLink();
  }

  @Override
  public String instruction() {
    return "<p>Which one <mark>is "
        + link.getLinkTypeLabel()
        + "</mark> a <em>DIFFERENT</em> "
        + categoryLink.getLinkType().nameOfSource
        + " <mark>"
        + categoryLink.getTargetNote().getTitle()
        + "</mark> than:";
  }

  @Override
  public String mainTopic() {
    return link.getSourceNote().getTitle();
  }
}
