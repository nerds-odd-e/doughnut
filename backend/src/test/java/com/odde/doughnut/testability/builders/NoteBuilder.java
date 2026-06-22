package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
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

  private List<PredefinedQuestionBuilder> predefinedQuestionBuilders = new ArrayList<>();
  private List<NoteBuilder> childrenBuilders = new ArrayList<>();
  private Folder folder;

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
    notebook.setName(notebookTestNameCounter.generate());
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
  }

  @Override
  protected void afterCreate(boolean needPersist) {
    childrenBuilders.forEach(bu -> bu.please(needPersist));
    predefinedQuestionBuilders.forEach(bu -> bu.please(needPersist));
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
        relationshipNoteMarkdownForEndpoints(entity, "a specialization of", from, to, null));
    return this;
  }

  private static final String RELATIONSHIP_NOTE_TYPE = "relationship";
  private static final String UNTITLED = "Untitled";
  private static final String DEFAULT_RELATION_LABEL = "related to";

  private static String relationshipNoteMarkdownForEndpoints(
      Note relationshipNote,
      String relationLabelOrNull,
      Note sourceEndpoint,
      Note targetEndpoint,
      String preservedDetailsOrNull) {
    String relationLabel = resolveRelationLabel(relationLabelOrNull);
    String relationKebab = relationKebabFromLabel(relationLabel);
    String sourceLink = wikiTokenForEndpoint(relationshipNote, sourceEndpoint);
    String targetLink = wikiTokenForEndpoint(relationshipNote, targetEndpoint);
    StringBuilder out = new StringBuilder();
    out.append("---\n");
    out.append("type: ").append(RELATIONSHIP_NOTE_TYPE).append('\n');
    out.append("relation: ").append(relationKebab).append('\n');
    out.append("source: \"").append(yamlDoubleQuotedInner(sourceLink)).append("\"\n");
    out.append("target: \"").append(yamlDoubleQuotedInner(targetLink)).append("\"\n");
    out.append("---\n\n");
    String preserved = trimmedOrNull(preservedDetailsOrNull);
    if (preserved != null) {
      out.append("\n\n").append(preserved);
    }
    return out.toString();
  }

  private static String resolveRelationLabel(String relationLabelOrNull) {
    if (relationLabelOrNull == null || trimmedOrEmpty(relationLabelOrNull).isEmpty()) {
      return DEFAULT_RELATION_LABEL;
    }
    return relationLabelOrNull.trim();
  }

  private static String relationKebabFromLabel(String label) {
    String t = trimmedOrEmpty(label);
    if (t.isEmpty()) {
      return relationKebabFromLabel(DEFAULT_RELATION_LABEL);
    }
    return t.toLowerCase().replaceAll("\\s+", "-");
  }

  private static String wikiTokenForEndpoint(Note relationshipNote, Note endpoint) {
    if (endpoint == null) {
      return wikiLink(UNTITLED);
    }
    String display = displayTitle(endpoint.getTitle());
    if (relationshipNote == null || sameNotebookAs(relationshipNote, endpoint)) {
      return wikiLink(display);
    }
    Notebook endNb = endpoint.getNotebook();
    if (endNb == null) {
      return wikiLink(display);
    }
    String nbName = trimmedOrEmpty(endNb.getName());
    if (nbName.isEmpty()) {
      return wikiLink(display);
    }
    return wikiLink(nbName + ": " + display);
  }

  private static boolean sameNotebookAs(Note relationshipNote, Note endpoint) {
    Notebook relNb = relationshipNote.getNotebook();
    Notebook endNb = endpoint.getNotebook();
    if (relNb == null || endNb == null) {
      return true;
    }
    Integer rid = relNb.getId();
    Integer eid = endNb.getId();
    if (rid != null && eid != null) {
      return rid.equals(eid);
    }
    return relNb == endNb;
  }

  private static String wikiLink(String displayTitle) {
    return "[[" + displayTitle + "]]";
  }

  private static String displayTitle(String title) {
    String t = trimmedOrEmpty(title);
    return t.isEmpty() ? UNTITLED : t;
  }

  private static String trimmedOrEmpty(String s) {
    if (s == null) {
      return "";
    }
    return s.trim();
  }

  private static String trimmedOrNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static String yamlDoubleQuotedInner(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
