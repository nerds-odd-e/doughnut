package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.algorithms.ClozeDescription;
import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.entities.QuizQuestion;

public class DescriptionLinkTargetQuizPresenter extends LinkTargetQuizPresenter {

  public DescriptionLinkTargetQuizPresenter(QuizQuestion quizQuestion) {
    super(quizQuestion);
  }

  @Override
  public String instruction() {
    ClozedString clozeDescription =
        ClozeDescription.htmlClosedDescription()
            .getClozeDescriptionXX(answerNote.getNoteTitle(), getSourceDescription());
    return "<p>The following descriptions is "
        + link.getLinkTypeLabel()
        + ":</p>"
        + "<pre style='white-space: pre-wrap;'>"
        + clozeDescription.cloze()
        + "</pre> ";
  }

  private ClozedString getSourceDescription() {
    return link.getSourceNote().getClozeDescription();
  }
}
