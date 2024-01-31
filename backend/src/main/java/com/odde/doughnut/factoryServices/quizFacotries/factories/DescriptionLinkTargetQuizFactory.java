package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Thing;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;

public class DescriptionLinkTargetQuizFactory extends LinkTargetQuizFactory
    implements SecondaryReviewPointsFactory {

  public DescriptionLinkTargetQuizFactory(Note note, QuizQuestionServant servant) {
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
  public Thing getCategoryLink() {
    return null;
  }
}
