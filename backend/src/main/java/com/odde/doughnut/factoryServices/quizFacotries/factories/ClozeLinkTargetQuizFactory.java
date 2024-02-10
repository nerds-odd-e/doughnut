package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionClozeLinkTarget;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class ClozeLinkTargetQuizFactory extends LinkTargetQuizFactory {

  public ClozeLinkTargetQuizFactory(LinkingNote note, QuizQuestionServant servant) {
    super(note, servant);
  }

  @Override
  public QuizQuestionEntity buildQuizQuestion() {
    return new QuizQuestionClozeLinkTarget();
  }
}
