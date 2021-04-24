package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

import java.sql.Timestamp;

public class NoteBuilder extends EntityBuilder<Note> {
    static final TestObjectCounter titleCounter = new TestObjectCounter(n->"title" + n);

    public NoteBuilder(Note note, MakeMe makeMe){
        super(makeMe, note);
        title(titleCounter.generate());
        description("descrption");
        createdAt(new Timestamp(System.currentTimeMillis()));
    }

    public NoteBuilder byUser(User user) {
        entity.setUser(user);
        return this;
    }

    public NoteBuilder byUser(UserModel userModel) {
        return byUser(userModel.getEntity());
    }

    public NoteBuilder under(Note parentNote) {
        entity.setParentNote(parentNote);
        return this;
    }

    public NoteBuilder linkTo(Note referTo) {
        return linkTo(referTo, Link.LinkType.BELONGS_TO);

    }


    public NoteBuilder linkTo(Note referTo, Link.LinkType linkType) {
        Link link = new Link();
        link.setTargetNote(referTo);
        link.setSourceNote(entity);
        link.setType(linkType.label);
        link.setUser(entity.getUser());
        entity.getLinks().add(link);
        referTo.getRefers().add(link);
        return this;

    }

    public NoteBuilder inCircle(CircleModel circleModel) {
        return inCircle(circleModel.getEntity());
    }

    public NoteBuilder inCircle(Circle circle) {
        buildNotebookUnlessExist();
        entity.getNotebook().setOwnership(circle.getOwnership());
        return this;
    }

    @Override
    protected void beforeCreate(boolean needPersist) {
        if (entity.getUser() == null) {
            Note parent = entity.getParentNote();
            if (parent != null && parent.getUser() != null) {
                byUser(parent.getUser());
            }else{
                byUser(makeMe.aUser().please(needPersist));
            }
        }

        buildNotebookUnlessExist();

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
        if(entity.getMasterReviewSetting() == null) {
            entity.setMasterReviewSetting(new ReviewSetting());
        }
        entity.getMasterReviewSetting().setRememberSpelling(true);
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

    private void buildNotebookUnlessExist() {
        if (entity.getNotebook() != null) {
            return;
        }
        Ownership ownership = null;
        if(entity.getUser() != null) {
            ownership = entity.getUser().getOwnership();
        }
        entity.buildNotebookForHeadNote(ownership, entity.getUser());
    }

    public NoteBuilder notebookOwnership(User user) {
        entity.getNotebook().setOwnership(user.getOwnership());
        return this;
    }

    public NoteBuilder underNotebook(Notebook nb) {
        entity.assignNotebookForHeadNote(nb, entity.getUser().getOwnership(), entity.getUser());
        return this;
    }
}
