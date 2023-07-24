package com.odde.doughnut.factoryServices.quizFacotries.implementation;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;

public class LinkTargetQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Link link;
  protected final Note answerNote;

  public LinkTargetQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getReviewPoint().getLink();
    this.answerNote = link.getTargetNote();
  }

  @Override
  public String mainTopic() {
    return "";
  }

  @Override
  public String instruction() {
    return "<mark>"
        + link.getSourceNote().getTitle()
        + "</mark> is "
        + link.getLinkTypeLabel()
        + ":";
  }
}
