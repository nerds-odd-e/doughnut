package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;

public class DescriptionLinkTargetQuizFactory extends LinkTargetQuizFactory {

  public DescriptionLinkTargetQuizFactory(LinkingNote note) {
    super(note);
  }

  @Override
  public void validateBasicPossibility() throws QuizQuestionNotPossibleException {
    super.validateBasicPossibility();
    if (!link.getParent().getClozeDescription().isPresent()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public String getStem() {
    ClozedString clozeDescription =
        link.getParent().getClozeDescription().hide(link.getTargetNote().getNoteTitle());
    return "<p>The following descriptions is "
        + link.getLinkType().label
        + ":</p>"
        + clozeDescription.clozeDetails();
  }
}
