package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.LinkingNote;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.entities.quizQuestions.QuizQuestionDescriptionLinkTarget;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class DescriptionLinkTargetQuizFactory extends LinkTargetQuizFactory
    implements SecondaryReviewPointsFactory {

  public DescriptionLinkTargetQuizFactory(LinkingNote note, QuizQuestionServant servant) {
    super(note, servant);
  }

  @Override
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    super.validatePossibility();
    if (!link.getParent().getClozeDescription().isPresent()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public LinkingNote getCategoryLink() {
    return null;
  }

  @Override
  public QuizQuestionEntity buildQuizQuestion() {
    return new QuizQuestionDescriptionLinkTarget();
  }
}
