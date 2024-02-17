package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.*;

public class FromSamePartAsQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Note link;
  private final Note categoryLink;

  public FromSamePartAsQuizPresenter(QuizQuestionWithNoteChoices quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getNote();
    this.categoryLink = quizQuestion.getCategoryLink();
  }

  @Override
  public String mainTopic() {
    return link.getParent().getTopicConstructor();
  }

  @Override
  public String stem() {
    return "<p>Which one <mark>is "
        + link.getLinkType().label
        + "</mark> the same "
        + categoryLink.getLinkType().nameOfSource
        + " of <mark>"
        + categoryLink.getTargetNote().getTopicConstructor()
        + "</mark> as:";
  }
}
