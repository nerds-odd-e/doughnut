package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.NoteBase;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.Thing;

public class LinkTargetQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Thing link;
  protected final NoteBase answerNote;

  public LinkTargetQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getThing();
    this.answerNote = link.getTargetNote();
  }

  @Override
  public String mainTopic() {
    return "";
  }

  @Override
  public String stem() {
    return "<mark>"
        + link.getSourceNote().getTopicConstructor()
        + "</mark> is "
        + link.getLinkTypeLabel()
        + ":";
  }
}
