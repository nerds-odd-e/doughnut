package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class DescriptionLinkTargetQuizFactory extends LinkTargetQuizFactory
    implements SecondaryReviewPointsFactory {

  public DescriptionLinkTargetQuizFactory(Thing thing, QuizQuestionServant servant) {
    super(thing, servant);
  }

  @Override
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    super.validatePossibility();
    if (!link.getSourceNote().getClozeDescription().isPresent()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public Link getCategoryLink() {
    return null;
  }
}
