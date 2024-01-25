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
        ClozedString.htmlClozedString(link.getSourceNote().getTopicConstructor())
            .hide(answerNote.getNoteTitle());
    return "<mark>" + clozeTitle.clozeTitle() + "</mark> is " + link.getLinkTypeLabel() + ":";
  }
}
