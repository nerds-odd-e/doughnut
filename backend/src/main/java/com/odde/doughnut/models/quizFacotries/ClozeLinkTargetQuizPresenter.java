package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.algorithms.ClozeDescription;
import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.entities.QuizQuestion;

public class ClozeLinkTargetQuizPresenter extends LinkTargetQuizPresenter {

  public ClozeLinkTargetQuizPresenter(QuizQuestion quizQuestion) {
    super(quizQuestion);
  }

  @Override
  public String instruction() {
    ClozedString clozeTitle =
        ClozeDescription.htmlClosedDescription()
            .getClozeDescription(answerNote.getNoteTitle(), link.getSourceNote().getTitle());
    return "<mark>" + clozeTitle.cloze() + "</mark> is " + link.getLinkTypeLabel() + ":";
  }
}
