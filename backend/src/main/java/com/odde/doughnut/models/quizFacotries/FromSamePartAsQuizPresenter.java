package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.*;
import java.util.List;

public class FromSamePartAsQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Link link;
  private final Link categoryLink;

  public FromSamePartAsQuizPresenter(QuizQuestionEntity quizQuestion) {
    this.link = quizQuestion.getReviewPoint().getLink();
    this.categoryLink = quizQuestion.getCategoryLink();
  }

  @Override
  public String mainTopic() {
    return link.getSourceNote().getTitle();
  }

  @Override
  public String instruction() {
    return "<p>Which one <mark>is "
        + link.getLinkTypeLabel()
        + "</mark> the same "
        + categoryLink.getLinkType().nameOfSource
        + " of <mark>"
        + categoryLink.getTargetNote().getTitle()
        + "</mark> as:";
  }
}
