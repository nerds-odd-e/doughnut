package com.odde.doughnut.models.quizFacotries;

import com.odde.doughnut.entities.EntityWithId;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.UserModel;
import java.util.List;

public interface QuestionOptionsFactory {
  EntityWithId generateAnswer();

  List<? extends EntityWithId> generateFillingOptions();

  default List<ReviewPoint> getViceReviewPoints(UserModel userModel) {
    return List.of();
  }

  default Link getCategoryLink() {
    return null;
  }
}
