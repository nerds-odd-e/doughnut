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

  List<NoteBuilder> reificationBuilders = new ArrayList<>();
  private List<PredefinedQuestionBuilder> predefinedQuestionBuilders = new ArrayList<>();
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

  public NoteBuilder asHeadNoteOfANotebook(Ownership ownership) {
    if (entity.getNotebook() != null)
      throw new AssertionError("Can add notebook for `" + entity + "`, a notebook already exist.");
    entity.buildNotebookForHeadNote(ownership, null);
    return this;
  }

  public NoteBuilder creatorAndOwner(User user) {
    if (entity.getNotebook() != null) {
      ownership(user.getOwnership());
    }
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

  public NoteBuilder ownership(Ownership ownership) {
    entity.getNotebook().setOwnership(ownership);
    return this;
  }

  public NoteBuilder under(Note parentNote) {
    entity.setParentNote(parentNote);
    if (entity.getCreator() == null) creator(parentNote.getCreator());
    parentNote.getNotebook().addNoteInMemoryToSupportUnitTestOnly(entity);
    return this;
  }

  public NoteBuilder linkTo(Note referTo) {
    return linkTo(referTo, LinkType.SPECIALIZE);
  }

  public NoteBuilder linkTo(Note referTo, LinkType linkType) {
    reificationBuilders.add(makeMe.aReification().between(entity, referTo, linkType));
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
    if (entity.getCreator() == null) {
      creator(makeMe.aUser().please(needPersist));
    }
    if (entity.getNotebook() == null) {
      asHeadNoteOfANotebook(entity.getCreator().getOwnership());
    }
    if (entity.getNotebook().getCreatorEntity() == null) {
      entity.getNotebook().setCreatorEntity(entity.getCreator());
    }
    if (entity.getNotebook().getOwnership() == null) {
      entity.getNotebook().setOwnership(entity.getCreator().getOwnership());
    }
    if (entity.getNotebook().getId() == null && needPersist) {
      makeMe.modelFactoryService.save(entity.getNotebook());
    }
  }

  @Override
  protected void afterCreate(boolean needPersist) {
    reificationBuilders.forEach(reificationBuilder -> reificationBuilder.please(needPersist));
    predefinedQuestionBuilders.forEach(bu -> bu.please(needPersist));
    childrenBuilders.forEach(bu -> bu.please(needPersist));
    if (reificationBuilders.isEmpty()
        && predefinedQuestionBuilders.isEmpty()
        && childrenBuilders.isEmpty()
        && !needPersist) return;
    if (entity.getTargetNote() != null) makeMe.refresh(entity.getParent());
    else makeMe.refresh(entity);
  }

  public NoteBuilder skipMemoryTracking() {
    entity.getRecallSetting().setSkipMemoryTracking(true);
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
    entity.getRecallSetting().setRememberSpelling(true);
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
    entity.getRecallSetting().setLevel(i);
    return this;
  }

  public NoteBuilder asFirstChildOf(Note note) {
    under(note);
    entity.updateSiblingOrderAsFirstChild(note);
    return this;
  }

  public NoteBuilder after(Note note) {
    under(note.getParent());
    entity.setSiblingOrderToInsertAfter(note);
    return this;
  }

  public NoteBuilder hasAnApprovedQuestion() {
    PredefinedQuestionBuilder predefinedQuestionBuilder =
        makeMe.aPredefinedQuestion().approvedQuestionOf(entity);
    this.predefinedQuestionBuilders.add(predefinedQuestionBuilder);
    return this;
  }

  public NoteBuilder hasAnUnapprovedQuestion() {
    PredefinedQuestionBuilder predefinedQuestionBuilder =
        makeMe.aPredefinedQuestion().ofAIGeneratedQuestionForNote(entity);
    this.predefinedQuestionBuilders.add(predefinedQuestionBuilder);
    return this;
  }

  public NoteBuilder hasApprovedQuestions(int numQuestions) {
    for (int i = 0; i < numQuestions; i++) {
      this.hasAnApprovedQuestion();
    }
    return this;
  }

  public NoteBuilder createdAt(Timestamp timestamp) {
    this.entity.setCreatedAt(timestamp);
    this.entity.setUpdatedAt(timestamp);
    return this;
  }
}
