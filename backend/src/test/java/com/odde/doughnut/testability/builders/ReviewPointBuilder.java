package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.models.ReviewPointModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

import java.sql.Timestamp;

public class ReviewPointBuilder extends EntityBuilder<ReviewPoint> {

    protected final Class<ReviewPointModel> mClass;

    public ReviewPointBuilder(ReviewPoint reviewPoint, MakeMe makeMe) {
        super(makeMe, reviewPoint);
        this.mClass = ReviewPointModel.class;
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

    public ReviewPointModel toModelPlease() {
        return makeMe.modelFactoryService.toModel(please(), mClass);
    }

    @Override
    protected void beforeCreate(boolean needPersist) {

    }

}
