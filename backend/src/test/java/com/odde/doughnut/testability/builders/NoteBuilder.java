package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.LinkEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.ReviewSettingEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

import java.sql.Timestamp;

public class NoteBuilder extends EntityBuilder<NoteEntity> {
    static final TestObjectCounter titleCounter = new TestObjectCounter(n->"title" + n);

    public NoteBuilder(NoteEntity noteEntity, MakeMe makeMe){
        super(makeMe, noteEntity);
        entity.setTitle(titleCounter.generate());
        entity.setDescription("descrption");
        entity.setUpdatedDatetime(new Timestamp(System.currentTimeMillis()));
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
        return this;
    }

    public NoteBuilder linkTo(NoteEntity referTo) {
        return linkTo(referTo, LinkEntity.LinkType.BELONGS_TO, null);

    }


    public NoteBuilder linkTo(NoteEntity referTo, LinkEntity.LinkType linkType, UserEntity userEntity) {
        if (userEntity == null) {
            userEntity = entity.getUserEntity();
        }
        LinkEntity linkEntity = new LinkEntity();
        linkEntity.setTargetNote(referTo);
        linkEntity.setSourceNote(entity);
        linkEntity.setType(linkType.label);
        linkEntity.setUserEntity(userEntity);
        entity.getLinks().add(linkEntity);
        referTo.getRefers().add(linkEntity);
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

    public NoteBuilder with10Children() {
        for(int i=0; i < 10; i++) {
            makeMe.aNote().under(entity).please();
        }
        return this;
    }

    public NoteBuilder rememberSpelling() {
        if(entity.getMasterReviewSettingEntity() == null) {
            entity.setMasterReviewSettingEntity(new ReviewSettingEntity());
        }
        entity.getMasterReviewSettingEntity().setRememberSpelling(true);
        return this;
    }

    public NoteBuilder createdAt(Timestamp timestamp) {
        entity.setCreatedDatetime(timestamp);
        return this;
    }

    public NoteBuilder withPicture(String picture) {
        entity.setPictureUrl(picture);
        return this;
    }

    public NoteBuilder useParentPicture() {
        entity.setUseParentPicture(true);
        return this;
    }

    public NoteBuilder withNewlyUploadedPicture() {
        entity.setUploadPictureProxy(makeMe.anUploadedPicture().toMultiplePartFilePlease());
        return this;
    }

    public NoteBuilder withTitle(String title) {
        entity.setTitle(title);
        return this;
    }
}
