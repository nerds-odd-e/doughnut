package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.entities.QuizQuestionEntity;

public class ClozeLinkTargetQuizPresenter extends LinkTargetQuizPresenter {

  public ClozeLinkTargetQuizPresenter(QuizQuestionEntity quizQuestion) {
    super(quizQuestion);
  }

  @Override
  public String instruction() {
    ClozedString clozeTitle =
        ClozedString.htmlClosedString(link.getSourceNote().getTitle())
            .hide(answerNote.getNoteTitle());
    return "<mark>" + clozeTitle.cloze() + "</mark> is " + link.getLinkTypeLabel() + ":";
  }
}
