package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.NoteConstructionService;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.logging.log4j.util.Strings;

public class NoteBuilder extends EntityBuilder<Note> {
  static final TestObjectCounter titleCounter = new TestObjectCounter(n -> "title" + n);

  UserBuilder creatorBuilder = null;
  List<LinkBuilder> linkBuilders = new ArrayList<>();
  private String audioFilename = null;
  private List<QuizQuestionBuilder> quizQuestionBuilders = new ArrayList<>();
  private List<NoteBuilder> childrenBuilders = new ArrayList<>();

  public NoteBuilder(Note note, MakeMe makeMe) {
    super(makeMe, note);
  }

  public NoteBuilder(MakeMe makeMe) {
    super(
        makeMe,
        new NoteConstructionService(null, new Timestamp(System.currentTimeMillis()), null)
            .createNote(null, ""));
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
    if (audioFilename != null) {
      entity
          .getOrInitializeNoteAccessory()
          .setAudioAttachment(makeMe.anAudio().name(audioFilename).please(needPersist));
    }
  }

  @Override
  protected void afterCreate(boolean needPersist) {
    linkBuilders.forEach(linkBuilder -> linkBuilder.please(needPersist));
    if (needPersist) makeMe.refresh(entity);
    quizQuestionBuilders.forEach(bu -> bu.please(needPersist));
    childrenBuilders.forEach(bu -> bu.please(needPersist));
    if (linkBuilders.isEmpty() && quizQuestionBuilders.isEmpty() && childrenBuilders.isEmpty())
      return;
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

  public NoteBuilder withNChildren(int numNotes) {
    return withNChildrenThat(numNotes, _ -> {});
  }

  public NoteBuilder withNChildrenThat(int numNotes, Consumer<NoteBuilder> childNoteThat) {
    for (int i = 0; i < numNotes; i++) {
      NoteBuilder childBuilder = makeMe.aNote().under(entity);
      childNoteThat.accept(childBuilder);
      this.childrenBuilders.add(childBuilder);
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

  public NoteBuilder imageUrl(String url) {
    entity.getOrInitializeNoteAccessory().setImageUrl(url);
    return this;
  }

  public NoteBuilder useParentImage() {
    entity.getOrInitializeNoteAccessory().setUseParentImage(true);
    return this;
  }

  public void withUploadedImage() {
    entity.getOrInitializeNoteAccessory().setImageAttachment(makeMe.anImage().please());
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

  public NoteBuilder audio(String filename) {
    this.audioFilename = filename;
    return this;
  }

  public NoteBuilder asFirstChildOf(Note note) {
    under(note);
    entity.updateSiblingOrder(note, true);
    return this;
  }

  public NoteBuilder after(Note note) {
    under(note.getParent());
    entity.updateSiblingOrder(note, false);
    return this;
  }

  public NoteBuilder hasAQuestion() {
    QuizQuestionBuilder quizQuestionBuilder = makeMe.aQuestion().spellingQuestionOfNote(entity);
    this.quizQuestionBuilders.add(quizQuestionBuilder);
    return this;
  }

  public NoteBuilder hasAnApprovedQuestion() {
    QuizQuestionBuilder quizQuestionBuilder =
        makeMe.aQuestion().spellingQuestionOfNote(entity).approveQuestion();
    this.quizQuestionBuilders.add(quizQuestionBuilder);
    return this;
  }
}
