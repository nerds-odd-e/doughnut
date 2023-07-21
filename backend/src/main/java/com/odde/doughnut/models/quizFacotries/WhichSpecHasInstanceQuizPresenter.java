package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.QuizQuestionEntity;

public class WhichSpecHasInstanceQuizPresenter extends QuizQuestionWithOptionsPresenter {
  private Link instanceLink;
  private final Link link;

  @Override
  public boolean isAnswerCorrect(String spellingAnswer) {
    return link.getSourceNote().matchAnswer(spellingAnswer);
  }

  public WhichSpecHasInstanceQuizPresenter(QuizQuestionEntity quizQuestion) {
    this.link = quizQuestion.getReviewPoint().getLink();
    this.instanceLink = quizQuestion.getCategoryLink();
  }

  @Override
  public String mainTopic() {
    return null;
  }

  @Override
  public String instruction() {
    return "<p>Which one is "
        + link.getLinkTypeLabel()
        + " <mark>"
        + link.getTargetNote().getTitle()
        + "</mark> <em>and</em> is "
        + instanceLink.getLinkTypeLabel()
        + " <mark>"
        + instanceLink.getTargetNote().getTitle()
        + "</mark>:";
  }
}
