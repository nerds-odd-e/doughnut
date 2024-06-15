package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class ClozeLinkTargetQuizFactory extends LinkTargetQuizFactory {

  public ClozeLinkTargetQuizFactory(LinkingNote note, QuizQuestionServant servant) {
    super(note, servant);
  }

  public String getStem() {
    ClozedString clozeTitle =
        ClozedString.htmlClozedString(link.getParent().getTopicConstructor())
            .hide(link.getTargetNote().getNoteTitle());
    return "<mark>" + clozeTitle.clozeTitle() + "</mark> is " + link.getLinkType().label + ":";
  }
}
