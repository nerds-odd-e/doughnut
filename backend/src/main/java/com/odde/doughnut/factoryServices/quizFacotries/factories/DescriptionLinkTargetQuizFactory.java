package com.odde.doughnut.factoryServices.quizFacotries.factories;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionServant;
import java.util.Collections;
import java.util.List;

public class DescriptionLinkTargetQuizFactory extends LinkTargetQuizFactory
    implements SecondaryReviewPointsFactory {

  private final QuizQuestionServant servant;

  public DescriptionLinkTargetQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    super(reviewPoint, servant);
    this.servant = servant;
  }

  @Override
  public void validatePossibility() throws QuizQuestionNotPossibleException {
    super.validatePossibility();
    if (!link.getSourceNote().getClozeDescription().isPresent()) {
      throw new QuizQuestionNotPossibleException();
    }
  }

  @Override
  public List<ReviewPoint> getViceReviewPoints() {
    ReviewPoint reviewPointFor = servant.getReviewPoint(link.getSourceNote().getThing());
    if (reviewPointFor != null) return List.of(reviewPointFor);
    return Collections.emptyList();
  }

  @Override
  public Link getCategoryLink() {
    return null;
  }
}
