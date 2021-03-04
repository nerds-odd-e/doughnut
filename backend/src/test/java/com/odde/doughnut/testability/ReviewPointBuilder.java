package com.odde.doughnut.testability;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewPointEntity;
import com.odde.doughnut.models.UserModel;

public class ReviewPointBuilder {
    private final MakeMe makeMe;
    private ReviewPointEntity reviewPointEntity;

    public ReviewPointBuilder(NoteEntity noteEntity, MakeMe makeMe) {
        reviewPointEntity = new ReviewPointEntity();
        reviewPointEntity.setNoteEntity(noteEntity);
        this.makeMe = makeMe;
    }

    public ReviewPointBuilder by(UserModel userModel) {
        reviewPointEntity.setUserEntity(userModel.getEntity());
        return this;
    }

    public ReviewPointEntity please() {
        makeMe.modelFactoryService.entityManager.persist(reviewPointEntity);
        return reviewPointEntity;
    }
}
