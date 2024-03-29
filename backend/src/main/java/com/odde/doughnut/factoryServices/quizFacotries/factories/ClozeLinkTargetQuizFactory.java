package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionClozeLinkTarget;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class ClozeLinkTargetQuizFactory extends LinkTargetQuizFactory {

  public ClozeLinkTargetQuizFactory(LinkingNote note) {
    super(note);
  }

  @Override
  public QuizQuestionClozeLinkTarget buildQuizQuestionObj(QuizQuestionServant servant) {
    QuizQuestionClozeLinkTarget quizQuestionClozeLinkTarget = new QuizQuestionClozeLinkTarget();
    quizQuestionClozeLinkTarget.setNote(link);
    return quizQuestionClozeLinkTarget;
  }
}
