package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.PredefinedQuestionServant;

public class DescriptionLinkTargetPredefinedFactory extends LinkTargetPredefinedFactory {

  public DescriptionLinkTargetPredefinedFactory(
      LinkingNote note, PredefinedQuestionServant servant) {
    super(note, servant);
  }

  @Override
  public void validateBasicPossibility() throws PredefinedQuestionNotPossibleException {
    super.validateBasicPossibility();
    if (!link.getParent().getClozeDescription().isPresent()) {
      throw new PredefinedQuestionNotPossibleException();
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
