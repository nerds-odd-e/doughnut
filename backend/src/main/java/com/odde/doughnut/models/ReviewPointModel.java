package com.odde.doughnut.models;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;

public record ReviewPointModel(ReviewPoint entity, ModelFactoryService modelFactoryService) {

  public ReviewPoint getEntity() {
    return entity;
  }

  public void initialReview(Timestamp currentUTCTimestamp, User user) {
    entity.setUser(user);
    entity.setInitialReviewedAt(currentUTCTimestamp);
    entity.setLastReviewedAt(currentUTCTimestamp);
    updateForgettingCurve(0);
  }

  public void markAsRepeated(Timestamp currentUTCTimestamp, boolean successful) {
    entity.setRepetitionCount(entity.getRepetitionCount() + 1);
    if (successful) {
      entity.reviewedSuccessfully(currentUTCTimestamp);
    } else {
      entity.reviewFailed(currentUTCTimestamp);
    }
    this.modelFactoryService.save(entity);
  }

  public void updateForgettingCurve(int adjustment) {
    entity.setForgettingCurveIndex(entity.getForgettingCurveIndex() + adjustment);
    entity.setNextReviewAt(entity.calculateNextReviewAt());
    this.modelFactoryService.save(entity);
  }
}
