package com.odde.doughnut.models;

import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;

public class ReviewPointModel extends ModelForEntity<ReviewPointEntity> {
    public ReviewPointModel(ReviewPointEntity entity, ModelFactoryService modelFactoryService) {
        super(entity, modelFactoryService);
    }

    public void initalReview(UserModel userModel, Timestamp currentUTCTimestamp) {
        getEntity().setInitialReviewedAt(currentUTCTimestamp);
        getEntity().setLastReviewedAt(currentUTCTimestamp);
        getEntity().setUserEntity(userModel.getEntity());
        modelFactoryService.reviewPointRepository.save(getEntity());
    }
}
