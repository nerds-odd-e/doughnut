package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.entities.QuizQuestionEntity;

public class ClozeLinkTargetQuizPresenter extends LinkTargetQuizPresenter {

  public ClozeLinkTargetQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
  }

  @Override
  public String stem() {
    ClozedString clozeTitle =
        ClozedString.htmlClosedString(link.getSourceNote().getTopic())
            .hide(answerNote.getNoteTitle());
    return "<mark>" + clozeTitle.cloze() + "</mark> is " + link.getLinkTypeLabel() + ":";
  }
}
