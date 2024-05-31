package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.entities.LinkingNote;

public class ClozeLinkTargetQuizFactory extends LinkTargetQuizFactory {

  public ClozeLinkTargetQuizFactory(LinkingNote note) {
    super(note);
  }

  public String getStem() {
    ClozedString clozeTitle =
        ClozedString.htmlClozedString(link.getParent().getTopicConstructor())
            .hide(link.getTargetNote().getNoteTitle());
    return "<mark>" + clozeTitle.clozeTitle() + "</mark> is " + link.getLinkType().label + ":";
  }
}
