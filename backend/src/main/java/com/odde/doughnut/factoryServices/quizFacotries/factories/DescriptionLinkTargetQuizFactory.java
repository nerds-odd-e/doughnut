package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionDescriptionLinkTarget;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

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
  public QuizQuestionEntity buildQuizQuestionObj(QuizQuestionServant servant) {
    QuizQuestionDescriptionLinkTarget quizQuestionDescriptionLinkTarget =
        new QuizQuestionDescriptionLinkTarget();
    quizQuestionDescriptionLinkTarget.setNote(link);
    return quizQuestionDescriptionLinkTarget;
  }
}
