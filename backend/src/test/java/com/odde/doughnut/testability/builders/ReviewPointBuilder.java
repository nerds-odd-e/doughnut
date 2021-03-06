package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.ModelForEntity;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityAndModelBuilder;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

import java.sql.Timestamp;

public class ReviewPointBuilder extends EntityAndModelBuilder<ReviewPointEntity, ReviewPointModel> {

    public ReviewPointBuilder(ReviewPointEntity reviewPointEntity, MakeMe makeMe) {
        super(makeMe, reviewPointEntity, ReviewPointModel.class);
    }

    public ReviewPointBuilder forNote(NoteEntity noteEntity) {
        entity.setNoteEntity(noteEntity);
        return this;
    }

    public ReviewPointBuilder by(UserModel userModel) {
        entity.setUserEntity(userModel.getEntity());
        return this;
    }

    public ReviewPointBuilder initiallyReviewedOn(Timestamp reviewTimestamp) {
        entity.setInitialReviewedAt(reviewTimestamp);
        entity.setLastReviewedAt(reviewTimestamp);
        return this;
    }

    public ReviewPointBuilder nthStrictRepetitionOn(Integer repetitionDone, Timestamp timestamp) {
        ReviewPointModel reviewPointModel = makeMe.modelFactoryService.toReviewPointModel(entity);
        for (int i = 0; i < repetitionDone + 1; i++) {
            reviewPointModel.repeat(timestamp);
        }
        return this;
    }
}
