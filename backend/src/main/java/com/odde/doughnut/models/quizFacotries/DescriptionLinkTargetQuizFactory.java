package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.UserModel;
import java.util.Collections;
import java.util.List;

public class DescriptionLinkTargetQuizFactory extends LinkTargetQuizFactory
    implements SecondaryReviewPointsFactory {

  private final User user;
  private final QuizQuestionServant servant;

  public DescriptionLinkTargetQuizFactory(ReviewPoint reviewPoint, QuizQuestionServant servant) {
    super(reviewPoint, servant);
    user = reviewPoint.getUser();
    this.servant = servant;
  }

  @Override
  public boolean isValidQuestion() throws QuizQuestionNotPossibleException {
    return super.isValidQuestion() && link.getSourceNote().getClozeDescription().isPresent();
  }

  @Override
  public List<ReviewPoint> getViceReviewPoints() {
    UserModel userModel = servant.modelFactoryService.toUserModel(user);
    ReviewPoint reviewPointFor = userModel.getReviewPointFor(link.getSourceNote());
    if (reviewPointFor != null) return List.of(reviewPointFor);
    return Collections.emptyList();
  }

  @Override
  public Link getCategoryLink() {
    return null;
  }
}
