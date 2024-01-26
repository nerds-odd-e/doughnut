package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Ownership;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import org.apache.logging.log4j.util.Strings;

public class NoteBuilder extends EntityBuilder<Note> {
  static final TestObjectCounter titleCounter = new TestObjectCounter(n -> "title" + n);

  UserBuilder creatorBuilder = null;

  public NoteBuilder(Note note, MakeMe makeMe) {
    super(makeMe, note);
  }

  public NoteBuilder(MakeMe makeMe) {
    super(makeMe, Note.createNote(null, new Timestamp(System.currentTimeMillis()), ""));
    if (Strings.isEmpty(entity.getTopicConstructor())) titleConstructor(titleCounter.generate());
    details("descrption");
    updatedAt(entity.getThing().getCreatedAt());
  }

  private void buildCreatorIfNotExist() {
    if (entity.getThing().getCreator() == null) {
      creatorBuilder = makeMe.aUser();
      creator(creatorBuilder.inMemoryPlease());
    }
  }

  public NoteBuilder asHeadNoteOfANotebook(Ownership ownership) {
    if (entity.getNotebook() != null)
      throw new AssertionError(
          "Can add notebook for `" + entity.toString() + "`, a notebook already exist.");
    buildCreatorIfNotExist();
    entity.buildNotebookForHeadNote(ownership, entity.getThing().getCreator());
    return this;
  }

  public NoteBuilder creatorAndOwner(User user) {
    return creator(user);
  }

  public NoteBuilder creator(User user) {
    if (entity.getThing().getCreator() != null)
      throw new AssertionError("creator already set for " + entity.toString());
    entity.getThing().setCreator(user);
    return this;
  }

  public NoteBuilder creatorAndOwner(UserModel userModel) {
    return creatorAndOwner(userModel.getEntity());
  }

  public NoteBuilder under(Note parentNote) {
    entity.setParentNote(parentNote);
    if (entity.getThing().getCreator() == null) creator(parentNote.getThing().getCreator());
    return this;
  }

  public NoteBuilder linkTo(Note referTo) {
    return linkTo(referTo, Link.LinkType.SPECIALIZE);
  }

  public NoteBuilder linkTo(Note referTo, Link.LinkType linkType) {
    makeMe.aLink().between(entity, referTo, linkType);
    return this;
  }

  public NoteBuilder inCircle(CircleModel circleModel) {
    return inCircle(circleModel.getEntity());
  }

  public NoteBuilder inCircle(Circle circle) {
    return asHeadNoteOfANotebook(circle.getOwnership());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getNotebook() == null) {
      buildCreatorIfNotExist();
      asHeadNoteOfANotebook(entity.getThing().getCreator().getOwnership());
    }
    if (entity.getThing().getCreator() == null) {
      creator(makeMe.aUser().please(needPersist));
    }
    if (creatorBuilder != null) creatorBuilder.please(needPersist);
  }

  public NoteBuilder skipReview() {
    ensureReviewSetting().setSkipReview(true);
    return this;
  }

  public NoteBuilder withNoDescription() {
    return details("");
  }

  public NoteBuilder titleConstructor(String text) {
    entity.setTopicConstructor(text);
    return this;
  }

  public NoteBuilder details(String text) {
    entity.setDetails(text);
    return this;
  }

  public NoteBuilder with10Children() {
    for (int i = 0; i < 10; i++) {
      makeMe.aNote().under(entity).please();
    }
    return this;
  }

  public NoteBuilder rememberSpelling() {
    ensureReviewSetting().setRememberSpelling(true);
    return this;
  }

  private ReviewSetting ensureReviewSetting() {
    if (entity.getMasterReviewSetting() == null) {
      entity.setMasterReviewSetting(new ReviewSetting());
    }
    return entity.getMasterReviewSetting();
  }

  public NoteBuilder updatedAt(Timestamp timestamp) {
    entity.setUpdatedAt(timestamp);
    return this;
  }

  public NoteBuilder pictureUrl(String picture) {
    entity.getNoteAccessories().setPictureUrl(picture);
    return this;
  }

  public NoteBuilder useParentPicture() {
    entity.getNoteAccessories().setUseParentPicture(true);
    return this;
  }

  public NoteBuilder withNewlyUploadedPicture() {
    entity
        .getNoteAccessories()
        .setUploadPictureProxy(makeMe.anUploadedPicture().toMultiplePartFilePlease());
    return this;
  }

  public void withUploadedPicture() {
    entity.getNoteAccessories().setUploadPicture(makeMe.anImage().please());
  }

  public NoteBuilder notebookOwnership(User user) {
    entity.getNotebook().setOwnership(user.getOwnership());
    return this;
  }

  public NoteBuilder softDeleted() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    entity.setDeletedAt(timestamp);
    return this;
  }

  public NoteBuilder wikidataId(String wikidataId) {
    entity.setWikidataId(wikidataId);
    return this;
  }

  public NoteBuilder target(Note targetNote, Link.LinkType linkType) {
    entity.setTargetNote(targetNote);
    entity.setTopicConstructor(":" + linkType.label);
    return this;
  }
}
