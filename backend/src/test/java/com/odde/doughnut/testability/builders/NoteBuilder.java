package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.util.Strings;

public class NoteBuilder extends EntityBuilder<Note> {
  static final TestObjectCounter titleCounter = new TestObjectCounter(n -> "title" + n);

  UserBuilder creatorBuilder = null;
  List<LinkBuilder> linkBuilders = new ArrayList<>();

  public NoteBuilder(Note note, MakeMe makeMe) {
    super(makeMe, note);
  }

  public NoteBuilder(MakeMe makeMe) {
    super(
        makeMe,
        HierarchicalNote.createNote(null, null, new Timestamp(System.currentTimeMillis()), ""));
    if (Strings.isEmpty(entity.getTopicConstructor())) titleConstructor(titleCounter.generate());
    details("descrption");
    updatedAt(entity.getCreatedAt());
  }

  private void buildCreatorIfNotExist() {
    if (entity.getCreator() == null) {
      creatorBuilder = makeMe.aUser();
      creator(creatorBuilder.inMemoryPlease());
    }
  }

  public NoteBuilder asHeadNoteOfANotebook(Ownership ownership) {
    if (entity.getNotebook() != null)
      throw new AssertionError("Can add notebook for `" + entity + "`, a notebook already exist.");
    buildCreatorIfNotExist();
    entity.buildNotebookForHeadNote(ownership, entity.getCreator());
    return this;
  }

  public NoteBuilder creatorAndOwner(User user) {
    return creator(user);
  }

  public NoteBuilder creator(User user) {
    if (entity.getCreator() != null)
      throw new AssertionError("creator already set for " + entity.toString());
    entity.setCreator(user);
    return this;
  }

  public NoteBuilder creatorAndOwner(UserModel userModel) {
    return creatorAndOwner(userModel.getEntity());
  }

  public NoteBuilder under(Note parentNote) {
    entity.setParentNote(parentNote);
    if (entity.getCreator() == null) creator(parentNote.getCreator());
    return this;
  }

  public NoteBuilder linkTo(Note referTo) {
    return linkTo(referTo, LinkType.SPECIALIZE);
  }

  public NoteBuilder linkTo(Note referTo, LinkType linkType) {
    linkBuilders.add(makeMe.aLink().between(entity, referTo, linkType));
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
    buildCreatorIfNotExist();
    if (entity.getCreator() == null) {
      creator(makeMe.aUser().please(needPersist));
    }
    if (creatorBuilder != null) creatorBuilder.please(needPersist);
    if (entity.getNotebook() == null) {
      asHeadNoteOfANotebook(entity.getCreator().getOwnership());
    }
    if (entity.getNotebook().getId() == null && needPersist) {
      makeMe.modelFactoryService.save(entity.getNotebook());
    }
  }

  @Override
  protected void afterCreate(boolean needPersist) {
    if (linkBuilders.isEmpty()) return;
    linkBuilders.forEach(linkBuilder -> linkBuilder.please(needPersist));
    if (needPersist) makeMe.refresh(entity);
  }

  public NoteBuilder skipReview() {
    entity.getReviewSetting().setSkipReview(true);
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
    entity.getReviewSetting().setRememberSpelling(true);
    return this;
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

  public NoteBuilder level(int i) {
    entity.getReviewSetting().setLevel(i);
    return this;
  }
}
