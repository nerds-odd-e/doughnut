package com.odde.doughnut.testability.builders;

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
    initiallyReviewedOn(makeMe.aTimestamp().of(0, 0).please());
  }

  public ReviewPointBuilder forNote(Note note) {
    entity.setThing(note.getThing());
    return this;
  }

  public ReviewPointBuilder forLink(Link link) {
    entity.setThing(link.getThing());
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
    entity.setNextReviewAt(reviewTimestamp);
    return this;
  }

  public ReviewPointBuilder afterNthStrictRepetition(Integer repetitionDone) {
    for (int i = 0; i < repetitionDone; i++) {
      entity.reviewedSuccessfully(entity.getNextReviewAt());
    }
    return this;
  }

  public ReviewPointModel toModelPlease() {
    return makeMe.modelFactoryService.toReviewPointModel(please());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {}

  public ReviewPointBuilder forgettingCurveAndNextReviewAt(int value) {
    entity.setForgettingCurveIndex(value);
    entity.setNextReviewAt(entity.calculateNextReviewAt());
    return this;
  }

  public ReviewPointBuilder removedFromReview() {
    entity.setRemovedFromReview(true);
    return this;
  }

  public ReviewPointBuilder nextReviewAt(Timestamp timestamp) {
    entity.setNextReviewAt(timestamp);
    return this;
  }
}
