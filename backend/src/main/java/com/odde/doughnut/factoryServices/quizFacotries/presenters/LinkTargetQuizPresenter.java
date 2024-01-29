package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteBase;
import com.odde.doughnut.entities.QuizQuestionEntity;

public class LinkTargetQuizPresenter extends QuizQuestionWithOptionsPresenter {
  protected final Link link;
  protected final NoteBase answerNote;

  public LinkTargetQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
    this.link = quizQuestion.getThing().getLink();
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
