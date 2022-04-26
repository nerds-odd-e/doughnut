package com.odde.doughnut.testability.builders;

import static com.odde.doughnut.entities.SelfEvaluate.satisfying;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;

public class ReviewPointBuilder extends EntityBuilder<ReviewPoint> {

  public ReviewPointBuilder(ReviewPoint reviewPoint, MakeMe makeMe) {
    super(makeMe, reviewPoint);
  }

  public ReviewPointBuilder forNote(Note note) {
    entity.setNote(note);
    return this;
  }

  public ReviewPointBuilder forLink(Link link) {
    entity.setLink(link);
    return this;
  }

  public ReviewPointBuilder by(UserModel userModel) {
    return by(userModel.getEntity());
  }

  public ReviewPointBuilder by(User user) {
    entity.setUser(user);
    return this;
  }

  public ReviewPointBuilder initiallyReviewedOn(Timestamp reviewTimestamp) {
    entity.setInitialReviewedAt(reviewTimestamp);
    entity.setLastReviewedAt(reviewTimestamp);
    return this;
  }

  public ReviewPointBuilder nthStrictRepetitionOn(Integer repetitionDone) {
    ReviewPointModel reviewPointModel = makeMe.modelFactoryService.toReviewPointModel(entity);
    for (int i = 0; i < repetitionDone; i++) {
      reviewPointModel.evaluate(entity.getNextReviewAt(), satisfying);
    }
    return this;
  }

  public ReviewPointModel toModelPlease() {
    return makeMe.modelFactoryService.toReviewPointModel(please());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public ReviewPointBuilder forgettiveCurveIndex(int value) {
    entity.setForgettingCurveIndex(value);
    return this;
  }

  public ReviewPointBuilder repetitionCount(int value) {
    entity.setRepetitionCount(value);
    return this;
  }
}
