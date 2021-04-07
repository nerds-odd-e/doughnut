package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

import java.sql.Timestamp;

public class NoteBuilder extends EntityBuilder<NoteEntity> {
    static final TestObjectCounter titleCounter = new TestObjectCounter(n->"title" + n);

    public NoteBuilder(NoteEntity noteEntity, MakeMe makeMe){
        super(makeMe, noteEntity);
        title(titleCounter.generate());
        description("descrption");
        createdAt(new Timestamp(System.currentTimeMillis()));
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
        return linkTo(referTo, LinkEntity.LinkType.BELONGS_TO);

    }


    public NoteBuilder linkTo(NoteEntity referTo, LinkEntity.LinkType linkType) {
        LinkEntity linkEntity = new LinkEntity();
        linkEntity.setTargetNote(referTo);
        linkEntity.setSourceNote(entity);
        linkEntity.setType(linkType.label);
        linkEntity.setUserEntity(entity.getUserEntity());
        entity.getLinks().add(linkEntity);
        referTo.getRefers().add(linkEntity);
        return this;

    }

    public NoteBuilder inCircle(CircleModel circleModel) {
        return inCircle(circleModel.getEntity());
    }

    public NoteBuilder inCircle(CircleEntity circleEntity) {
        entity.setOwnershipEntity(circleEntity.getOwnershipEntity());
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
            ownership(entity.getUserEntity());
        }

    }

    public NoteBuilder skipReview() {
        entity.getNoteContent().setSkipReview(true);
        return this;
    }

    public NoteBuilder withNoDescription() {
        return description("");
    }

    public NoteBuilder title(String text) {
        entity.getNoteContent().setTitle(text);
        return this;
    }

    public NoteBuilder description(String text) {
        entity.getNoteContent().setDescription(text);
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
        entity.getNoteContent().setCreatedDatetime(timestamp);
        return this;
    }

    public NoteBuilder pictureUrl(String picture) {
        entity.getNoteContent().setPictureUrl(picture);
        return this;
    }

    public NoteBuilder useParentPicture() {
        entity.getNoteContent().setUseParentPicture(true);
        return this;
    }

    public NoteBuilder withNewlyUploadedPicture() {
        entity.getNoteContent().setUploadPictureProxy(makeMe.anUploadedPicture().toMultiplePartFilePlease());
        return this;
    }

    public void withUploadedPicture() {
        entity.getNoteContent().setUploadPicture(makeMe.anImage().please());
    }

    public NoteBuilder ownership(UserEntity userEntity) {
        entity.setOwnershipEntity(userEntity.getOwnershipEntity());
        return this;
    }

    public NoteBuilder asTheHeadNoteOfANotebook() {
        NotebookEntity notebookEntity = new NotebookEntity();
        notebookEntity.setOwnershipEntity(entity.getUserEntity().getOwnershipEntity());
        notebookEntity.setCreatorEntity(entity.getUserEntity());
        notebookEntity.setHeadNoteEntity(entity);
        entity.setNotebookEntity(notebookEntity);

        return this;
    }
}
