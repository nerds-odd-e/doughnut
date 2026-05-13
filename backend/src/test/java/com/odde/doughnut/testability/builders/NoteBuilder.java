package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.RelationshipNoteMarkdownFormatter;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.logging.log4j.util.Strings;

public class NoteBuilder extends EntityBuilder<Note> {
  static final TestObjectCounter titleCounter = new TestObjectCounter(n -> "title" + n);
  static final TestObjectCounter notebookTestNameCounter =
      new TestObjectCounter(n -> "notebook" + n);

  List<NoteBuilder> relationBuilders = new ArrayList<>();
  private List<PredefinedQuestionBuilder> predefinedQuestionBuilders = new ArrayList<>();
  private List<NoteBuilder> childrenBuilders = new ArrayList<>();
  private Folder folder;

  public NoteBuilder(Note note, MakeMe makeMe) {
    super(makeMe, note);
  }

  public NoteBuilder(MakeMe makeMe) {
    super(makeMe, new Note());
    entity.initializeNewNote(null, null, new Timestamp(System.currentTimeMillis()), "");
    if (Strings.isEmpty(entity.getTitle())) title(titleCounter.generate());
    content("descrption");
    updatedAt(entity.getCreatedAt());
  }

  private NoteBuilder attachToNewNotebook(Ownership ownership) {
    if (entity.getNotebook() != null)
      throw new AssertionError("Can add notebook for `" + entity + "`, a notebook already exist.");
    Notebook notebook = new Notebook();
    notebook.setCreator(null);
    notebook.setOwnership(ownership);
    Timestamp ts =
        entity.getCreatedAt() != null
            ? entity.getCreatedAt()
            : new Timestamp(System.currentTimeMillis());
    notebook.setCreatedAt(ts);
    notebook.setUpdatedAt(ts);
    notebook.setName(notebookTestNameCounter.generate());
    return notebook(notebook);
  }

  public NoteBuilder notebook(Notebook notebook) {
    if (entity.getNotebook() == null) {
      entity.assignNotebook(notebook);
      return this;
    }
    if (folder != null) {
      throw new AssertionError(
          "Don't set folder and notebook at the same time. It leads to inconsistency in test.");
    }
    throw new AssertionError("Notebook already set for `" + entity + "`.");
  }

  public NoteBuilder notebookOwnedBy(User user) {
    if (entity.getNotebook() != null) {
      throw new AssertionError(
          "Notebook already set for `" + entity + "`, cannot set creator and owner for notebook.");
    }
    attachToNewNotebook(user.getOwnership());
    return this;
  }

  public NoteBuilder creator(User user) {
    if (entity.getCreator() != null)
      throw new AssertionError("creator already set for " + entity.toString());
    entity.setCreator(user);
    return this;
  }

  public NoteBuilder underSameNotebookAs(Note note) {
    notebook(note.getNotebook());
    note.getNotebook().addNoteInMemoryToSupportUnitTestOnly(entity);
    return this;
  }

  public NoteBuilder inCircle(Circle circle) {
    return attachToNewNotebook(circle.getOwnership());
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    if (entity.getCreator() == null) {
      creator(makeMe.aUser().please(needPersist));
    }
    if (entity.getNotebook() == null) {
      attachToNewNotebook(entity.getCreator().getOwnership());
    }
    NotebookBuilder notebookBuilder = new NotebookBuilder(entity.getNotebook(), makeMe);
    entity.assignNotebook(notebookBuilder.please(needPersist));
    if (folder != null) {
      entity.setFolder(folder);
    }
  }

  @Override
  protected void afterCreate(boolean needPersist) {
    relationBuilders.forEach(relationBuilder -> relationBuilder.please(needPersist));
    predefinedQuestionBuilders.forEach(bu -> bu.please(needPersist));
    childrenBuilders.forEach(bu -> bu.please(needPersist));
    if (relationBuilders.isEmpty()
        && predefinedQuestionBuilders.isEmpty()
        && childrenBuilders.isEmpty()
        && !needPersist) return;
    makeMe.refresh(entity);
  }

  public NoteBuilder skipMemoryTracking() {
    entity.getRecallSetting().setSkipMemoryTracking(true);
    return this;
  }

  public NoteBuilder folder(Folder folder) {
    if (entity.getNotebook() != null) {
      throw new AssertionError(
          "Don't set folder and notebook at the same time. It leads to inconsistency in test.");
    }
    notebook(folder.getNotebook());
    this.folder = folder;
    return this;
  }

  public NoteBuilder title(String text) {
    entity.setTitle(text);
    return this;
  }

  public NoteBuilder content(String text) {
    entity.setContent(text);
    return this;
  }

  public NoteBuilder withNChildren(int numNotes) {
    return withNChildrenThat(numNotes, _ -> {});
  }

  public NoteBuilder withNChildrenThat(int numNotes, Consumer<NoteBuilder> childNoteThat) {
    for (int i = 0; i < numNotes; i++) {
      NoteBuilder childBuilder = makeMe.aNote().underSameNotebookAs(entity);
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

  public NoteBuilder notebookOwnership(User user) {
    entity.getNotebook().setOwnership(user.getOwnership());
    return this;
  }

  public NoteBuilder softDeleted() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    entity.setDeletedAt(timestamp);
    return this;
  }

  public NoteBuilder level(int i) {
    entity.getRecallSetting().setLevel(i);
    return this;
  }

  public NoteBuilder hasAPredefinedQuestion() {
    PredefinedQuestionBuilder predefinedQuestionBuilder =
        makeMe.aPredefinedQuestion().ofAIGeneratedQuestionForNote(entity);
    this.predefinedQuestionBuilders.add(predefinedQuestionBuilder);
    return this;
  }

  public NoteBuilder createdAt(Timestamp timestamp) {
    this.entity.setCreatedAt(timestamp);
    this.entity.setUpdatedAt(timestamp);
    return this;
  }

  public NoteBuilder withWikiLinksInFrontmatter(Note from, Note to) {
    entity.setContent(
        RelationshipNoteMarkdownFormatter.formatForRelationshipNote(
            entity, "a specialization of", from, to, null));
    return this;
  }
}
