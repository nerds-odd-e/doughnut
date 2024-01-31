package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.Thing;

public class WhichSpecHasInstanceQuizPresenter extends QuizQuestionWithOptionsPresenter {
  private Thing instanceLink;
  private final Note link;

  public WhichSpecHasInstanceQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getNote();
    this.instanceLink = quizQuestion.getCategoryLink();
  }

  @Override
  public String mainTopic() {
    return null;
  }

  @Override
  public String stem() {
    return "<p>Which one is "
        + link.getLinkType().label
        + " <mark>"
        + link.getTargetNote().getTopicConstructor()
        + "</mark> <em>and</em> is "
        + instanceLink.getLinkTypeLabel()
        + " <mark>"
        + instanceLink.getTargetNote().getTopicConstructor()
        + "</mark>:";
  }
}
