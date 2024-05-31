package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class ClozeLinkTargetQuizFactory extends LinkTargetQuizFactory {

  public ClozeLinkTargetQuizFactory(LinkingNote note) {
    super(note);
  }

  @Override
  public QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant) {
    QuizQuestionEntity quizQuestionClozeLinkTarget = new QuizQuestionEntity();
    quizQuestionClozeLinkTarget.setNote(link);
    return quizQuestionClozeLinkTarget;
  }

  public String getStem() {
    ClozedString clozeTitle =
        ClozedString.htmlClozedString(link.getParent().getTopicConstructor())
            .hide(link.getTargetNote().getNoteTitle());
    return "<mark>" + clozeTitle.clozeTitle() + "</mark> is " + link.getLinkType().label + ":";
  }
}
