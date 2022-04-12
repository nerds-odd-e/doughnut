package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class DescriptionLinkTargetQuizFactory extends LinkTargetQuizFactory {

  public DescriptionLinkTargetQuizFactory(ReviewPoint reviewPoint) {
    super(reviewPoint);
  }

  private String getSourceDescription() {
    return link.getSourceNote().getClozeDescription();
  }

  @Override
  public boolean isValidQuestion() {
    return super.isValidQuestion() && Strings.isNotEmpty(getSourceDescription());
  }

  @Override
  public List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
    ReviewPoint reviewPointFor = userModel.getReviewPointFor(link.getSourceNote());
    if (reviewPointFor != null) return List.of(reviewPointFor);
    return Collections.emptyList();
  }
}
