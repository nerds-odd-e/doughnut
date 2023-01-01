package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.UserModel;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

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
  public boolean isValidQuestion() {
    return super.isValidQuestion()
        && Strings.isNotEmpty(link.getSourceNote().getClozeDescription().cloze());
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
