package com.odde.doughnut.factoryServices.quizFacotries.presenters;

import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.entities.QuizQuestionWithNoteChoices;

public class ClozeLinkTargetQuizPresenter extends LinkTargetQuizPresenter {

  public ClozeLinkTargetQuizPresenter(QuizQuestionWithNoteChoices quizQuestion) {
    super(quizQuestion);
  }

  @Override
  public String stem() {
    ClozedString clozeTitle =
        ClozedString.htmlClozedString(link.getParent().getTopicConstructor())
            .hide(answerNote.getNoteTitle());
    return "<mark>" + clozeTitle.clozeTitle() + "</mark> is " + link.getLinkType().label + ":";
  }
}
