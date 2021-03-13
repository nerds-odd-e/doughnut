package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

import java.beans.BeanProperty;
import java.time.LocalDate;

public class NoteBuilder extends EntityBuilder<NoteEntity> {
    static final TestObjectCounter titleCounter = new TestObjectCounter(n->"title" + n);

    public NoteBuilder(NoteEntity noteEntity, MakeMe makeMe){
        super(makeMe, noteEntity);
        entity.setTitle(titleCounter.generate());
        entity.setDescription("descrption");
        entity.setUpdatedDatetime(java.sql.Date.valueOf(LocalDate.now()));
    }

    public NoteBuilder byUser(UserEntity userEntity) {
        entity.setUserEntity(userEntity);
        return this;
    }

    public NoteBuilder byUser(UserModel userModel) {
        return byUser(userModel.getEntity());
    }

    public NoteBuilder under(NoteEntity parentNote) {
        entity.setParentNote(parentNote);
        parentNote.getChildren().add(entity);
        return this;
    }

    public NoteBuilder linkTo(NoteEntity referTo) {
        LinkEntity linkEntity = new LinkEntity();
        linkEntity.setTargetNote(referTo);
        linkEntity.setSourceNote(entity);
        linkEntity.setType("belongs to");
        entity.getLinks().add(linkEntity);
        return this;
    }

    public NoteBuilder inCircle(CircleModel circleModel) {
        entity.setOwnershipEntity(circleModel.getEntity().getOwnershipEntity());
        return this;
    }

    @Override
    protected void beforeCreate(boolean needPersist) {
        if (entity.getUserEntity() == null) {
            NoteEntity parent = entity.getParentNote();
            if (parent != null && parent.getUserEntity() != null) {
                byUser(parent.getUserEntity());
            }else{
                byUser(makeMe.aUser().please(needPersist));
            }
        }
        if (entity.getOwnershipEntity() == null) {
            entity.setOwnershipEntity(entity.getUserEntity().getOwnershipEntity());
        }

    }

    public NoteBuilder skipReview() {
        entity.setSkipReview(true);
        return this;
    }

    public NoteBuilder withNoDescription() {
        entity.setDescription("");
        return this;
    }

    public NoteBuilder title(String text) {
        entity.setTitle(text);
        return this;
    }

    public NoteBuilder description(String text) {
        entity.setDescription(text);
        return this;
    }

}
