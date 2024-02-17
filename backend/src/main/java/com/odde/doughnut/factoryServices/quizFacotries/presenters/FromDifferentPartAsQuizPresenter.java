package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.*;

public class FromDifferentPartAsQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Note link;
  private Note categoryLink;

  public FromDifferentPartAsQuizPresenter(QuizQuestionWithNoteChoices quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getNote();
    this.categoryLink = quizQuestion.getCategoryLink();
  }

  @Override
  public String stem() {
    return "<p>Which one <mark>is "
        + link.getLinkType().label
        + "</mark> a <em>DIFFERENT</em> "
        + categoryLink.getLinkType().nameOfSource
        + " of <mark>"
        + categoryLink.getTargetNote().getTopicConstructor()
        + "</mark> than:";
  }

  @Override
  public String mainTopic() {
    return link.getParent().getTopicConstructor();
  }
}
