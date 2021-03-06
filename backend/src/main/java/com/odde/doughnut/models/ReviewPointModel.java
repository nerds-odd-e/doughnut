package com.odde.doughnut.models;

import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.services.ModelFactoryService;

import java.sql.Timestamp;

public class ReviewPointModel extends ModelForEntity<ReviewPointEntity> {
    public ReviewPointModel(ReviewPointEntity entity, ModelFactoryService modelFactoryService) {
        super(entity, modelFactoryService);
    }

    public void initialReview(UserModel userModel, Timestamp currentUTCTimestamp) {
        getEntity().setUserEntity(userModel.getEntity());
        getEntity().setInitialReviewedAt(currentUTCTimestamp);
        repeat(currentUTCTimestamp);
    }

    public void repeat(Timestamp currentUTCTimestamp) {
        getEntity().repeatedOnTime();
        getEntity().setLastAndNextReviewAt(getUserModel().getSpacedRepetition(), currentUTCTimestamp);
        this.modelFactoryService.reviewPointRepository.save(getEntity());
    }

    private UserModel getUserModel() {
        return modelFactoryService.toUserModel(getEntity().getUserEntity());
    }

}
