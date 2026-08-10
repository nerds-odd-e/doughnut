package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.RelationshipNoteMarkdown;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.apache.logging.log4j.util.Strings;

public class NoteBuilder extends EntityBuilder<Note> {
  static final TestObjectCounter titleCounter = new TestObjectCounter(n -> "title" + n);
  static final TestObjectCounter notebookTestNameCounter =
      new TestObjectCounter(n -> "notebook" + n);

  private List<PredefinedQuestionBuilder> predefinedQuestionBuilders = new ArrayList<>();
  private List<NoteBuilder> childrenBuilders = new ArrayList<>();
  private Folder folder;
  private final NoteFrontmatterLists frontmatterLists = new NoteFrontmatterLists();

  public NoteBuilder(Note note, MakeMe makeMe) {
    super(makeMe, note);
  }

  public NoteBuilder(MakeMe makeMe) {
    super(makeMe, new Note());
    entity.initializeNewNote(null, new Timestamp(System.currentTimeMillis()), "");
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
    notebook.setName(new DisplayName(notebookTestNameCounter.generate()));
    return notebook(notebook);
  }

  private NoteBuilder attachToNewNotebookOwnedBy(User user) {
    attachToNewNotebook(user.getOwnership());
    entity.getNotebook().setCreator(user);
    return this;
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
    return attachToNewNotebookOwnedBy(user);
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
    NotebookBuilder notebookBuilder = new NotebookBuilder(entity.getNotebook(), makeMe);
    entity.assignNotebook(notebookBuilder.please(needPersist));
    if (folder != null) {
      entity.setFolder(folder);
    }
    applyPendingFrontmatterLists();
  }

  @Override
  protected void afterCreate(boolean needPersist) {
    childrenBuilders.forEach(bu -> bu.please(needPersist));
    predefinedQuestionBuilders.forEach(bu -> bu.please(needPersist));
    if (needPersist
        && frontmatterLists.shouldRefreshAliasIndex()
        && makeMe.noteAliasIndexService != null) {
      makeMe.noteAliasIndexService.refreshForNote(entity);
    }
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
    entity.setTitle(new DisplayName(text));
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

  /** Adds plain frontmatter aliases and refreshes the alias index after persist. */
  public NoteBuilder aliases(String... aliases) {
    frontmatterLists.addPlainAliases(aliases);
    return this;
  }

  /**
   * Declares an overlap under frontmatter {@code overlaps} targeting {@code partner} ({@code
   * notebook:title} form). Partner must already be persisted so its notebook name is available.
   */
  public NoteBuilder overlapPartner(Note partner) {
    frontmatterLists.addOverlapPartner(partner);
    return this;
  }

  /**
   * Declares an overlap under frontmatter {@code overlaps} with the given inner token (e.g. {@code
   * Title} or {@code NB:Title}).
   */
  public NoteBuilder overlapWikiLink(String wikiLinkInner) {
    frontmatterLists.addOverlapWikiLink(wikiLinkInner);
    return this;
  }

  /**
   * Puts a wiki-link item under frontmatter {@code aliases} targeting {@code partner}. Invalid for
   * authored save; used only for read-path fixtures.
   */
  public NoteBuilder wikiLinkUnderAliasesPartner(Note partner) {
    frontmatterLists.addWikiLinkUnderAliasesPartner(partner);
    return this;
  }

  /**
   * Puts a wiki-link item under frontmatter {@code aliases}. Invalid for authored save; used only
   * for read-path fixtures.
   */
  public NoteBuilder wikiLinkUnderAliases(String wikiLinkInner) {
    frontmatterLists.addWikiLinkUnderAliases(wikiLinkInner);
    return this;
  }

  private void applyPendingFrontmatterLists() {
    String composed = frontmatterLists.composedContentOrEmpty();
    if (!composed.isEmpty()) {
      content(composed);
    }
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
    return asRelationship("a specialization of", from, to);
  }

  public NoteBuilder asRelationship(String relationLabel, Note source, Note target) {
    entity.setContent(
        RelationshipNoteMarkdown.forEndpoints(entity, relationLabel, source, target, null));
    return this;
  }
}
