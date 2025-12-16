package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.algorithms.HtmlOrMarkdown;
import com.odde.doughnut.algorithms.NoteTitle;
import com.odde.doughnut.algorithms.SiblingOrder;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteTopology;
import com.odde.doughnut.entities.converters.NoteTypeConverter;
import com.odde.doughnut.services.graphRAG.BareNote;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "note")
@JsonPropertyOrder({"noteTopology", "details", "parentId", "relationType", "updatedAt"})
public class Note extends EntityIdentifiedByIdOnly {
  public static final int MAX_TITLE_LENGTH = 150;
  public static final String NOTE_OF_CURRENT_FOCUS = "note of current focus";

  @OneToOne
  @JoinColumn(name = "creator_id")
  @JsonIgnore
  @Getter
  @Setter
  private User creator;

  @ManyToOne
  @JoinColumn(name = "notebook_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  private Notebook notebook;

  @OneToOne(mappedBy = "note", cascade = CascadeType.ALL)
  @JsonIgnore
  @Getter
  private NoteAccessory noteAccessory;

  @Column(name = "details")
  @Getter
  @Setter
  @JsonPropertyDescription("The details of the note is in markdown format.")
  private String details;

  @Size(min = 1, max = Note.MAX_TITLE_LENGTH)
  @Getter
  @Setter
  @Column(name = "title")
  @NotNull
  @JsonIgnore
  private String title = "";

  @Column(name = "created_at")
  @Setter
  @Getter
  @NotNull
  private Timestamp createdAt;

  @Setter
  @Column(name = "deleted_at")
  @Getter
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Timestamp deletedAt;

  @OneToMany(mappedBy = "targetNote")
  private List<Note> inboundReferences = new ArrayList<>();

  @OneToMany(mappedBy = "parent", cascade = CascadeType.DETACH)
  @JsonIgnore
  @OrderBy("siblingOrder")
  private final List<Note> children = new ArrayList<>();

  @OneToMany(mappedBy = "note")
  @JsonIgnore
  private Set<MemoryTracker> memoryTrackers;

  @Column(name = "updated_at")
  @Getter
  @Setter
  @NotNull
  private Timestamp updatedAt;

  @Column(name = "wikidata_id")
  @Getter
  @Setter
  private String wikidataId;

  @Column(name = "sibling_order")
  @JsonIgnore
  @Getter
  private Long siblingOrder = SiblingOrder.getGoodEnoughOrderNumber();

  @ManyToOne
  @JoinColumn(name = "target_note_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Note targetNote;

  @OneToOne
  @JoinColumn(name = "parent_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  private Note parent;

  @Convert(converter = NoteTypeConverter.class)
  @Column(name = "note_type")
  @JsonIgnore
  @Getter
  @Setter
  private NoteType noteType = NoteType.UNASSIGNED;

  @OneToMany(mappedBy = "note")
  @Getter
  @JsonIgnore
  private List<PredefinedQuestion> predefinedQuestions = new ArrayList<>();

  @Embedded @JsonIgnore @Getter private NoteRecallSetting recallSetting = new NoteRecallSetting();

  @JsonIgnore
  public List<Note> getChildren() {
    return filterDeletedUnmodifiableNoteList(children);
  }

  @JsonIgnore
  public List<Note> getRelationships() {
    return getChildren().stream().filter(Note::isRelation).toList();
  }

  @JsonIgnore
  public List<Note> getInboundReferences() {
    return filterDeletedUnmodifiableNoteList(inboundReferences);
  }

  public static <T extends Note> List<T> filterDeletedUnmodifiableNoteList(List<T> notes) {
    return notes.stream().filter(n -> n.getDeletedAt() == null).toList();
  }

  @JsonIgnore
  public NoteTitle getNoteTitle() {
    return new NoteTitle(getTitle());
  }

  @JsonIgnore
  public List<Note> getSiblings() {
    if (getParent() == null) {
      return new ArrayList<>();
    }
    return getParent().getChildren();
  }

  @JsonIgnore
  public ClozedString getClozeDescription() {
    if (isDetailsBlankHtml()) return new ClozedString(null, "");

    return ClozedString.htmlClozedString(getDetails()).hide(getNoteTitle());
  }

  @JsonIgnore
  public boolean isDetailsBlankHtml() {
    return new HtmlOrMarkdown(getDetails()).isBlank();
  }

  @JsonIgnore
  public RelationType getRelationType() {
    if (!getTitle().startsWith(":")) return null;
    return RelationType.fromLabel(getTitle().substring(1));
  }

  @JsonIgnore
  public void setRelationType(RelationType relationType) {
    setTitle(":" + relationType.label);
  }

  @JsonIgnore
  public List<Note> getAncestors() {
    List<Note> result = new ArrayList<>();
    Note p = getParent();
    while (p != null) {
      result.add(0, p);
      p = p.getParent();
    }
    return result;
  }

  @Override
  public String toString() {
    return "Note{" + "id=" + id + ", title='" + getTitle() + '\'' + '}';
  }

  @JsonIgnore
  public void setParentNote(Note parentNote) {
    if (parentNote == null) return;
    setNotebook(parentNote.getNotebook());
    this.parent = parentNote;
    // Update notebook for all descendants including relationships
    getAllDescendants().forEach(descendant -> descendant.setNotebook(parentNote.getNotebook()));
  }

  private void setNotebook(Notebook notebook) {
    this.notebook = notebook;
  }

  @JsonIgnore
  public void setSiblingOrderToInsertAfter(Note relativeToNote) {
    this.siblingOrder =
        relativeToNote
            .nextSibling()
            .map(x -> (relativeToNote.siblingOrder + x.getSiblingOrder()) / 2)
            .orElse(relativeToNote.siblingOrder + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT);
  }

  public void updateSiblingOrderAsFirstChild(Note parentNote) {
    parentNote.getChildren().stream()
        .findFirst()
        .ifPresent(
            firstChild ->
                this.siblingOrder =
                    firstChild.getSiblingOrder() - SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT);
  }

  private Optional<Note> nextSibling() {
    return getSiblings().stream().filter(nc -> nc.getSiblingOrder() > siblingOrder).findFirst();
  }

  public Integer getParentId() {
    Note parent = getParent();
    if (parent == null) return null;
    return parent.id;
  }

  @JsonIgnore
  public ImageWithMask getImageWithMask() {
    if (this.noteAccessory == null) return null;

    return noteAccessory.getImageWithMask();
  }

  public void prependDescription(String addition) {
    String prevDesc = getDetails() != null ? getDetails() : "";
    String desc = prevDesc.isEmpty() ? addition : addition + "\n" + prevDesc;
    setDetails(desc);
  }

  @JsonIgnore
  public boolean matchAnswer(String spellingAnswer) {
    return getNoteTitle().matches(spellingAnswer);
  }

  @JsonIgnore
  public Stream<Note> getAllDescendants() {
    return Stream.concat(
        getChildren().stream(), getChildren().stream().flatMap(Note::getAllDescendants));
  }

  @JsonIgnore
  public Stream<Note> getAllNoneRelationDescendants() {
    return getAllDescendants().filter(n -> !n.isRelation());
  }

  @JsonIgnore
  public Stream<Note> getRelationshipsAndRefers() {
    return Stream.concat(getRelationships().stream(), getInboundReferences().stream());
  }

  @JsonIgnore
  public NoteAccessory getOrInitializeNoteAccessory() {
    if (noteAccessory == null) {
      noteAccessory = new NoteAccessory();
      noteAccessory.setNote(this);
    }
    return noteAccessory;
  }

  @NonNull
  public NoteTopology getNoteTopology() {
    NoteTopology noteTopology = new NoteTopology();
    noteTopology.setId(getId());
    noteTopology.setTitleOrPredicate(getTitle());
    noteTopology.setShortDetails(getShortDetails());
    noteTopology.setRelationType(getRelationType());
    if (getParent() != null) {
      noteTopology.setParentOrSubjectNoteTopology(getParent().getNoteTopology());
    }
    if (getTargetNote() != null) {
      noteTopology.setTargetNoteTopology(getTargetNote().getNoteTopology());
    }
    return noteTopology;
  }

  @JsonIgnore
  public String getShortDetails() {
    return new HtmlOrMarkdown(getDetails()).beginning(50);
  }

  @JsonIgnore
  public void adjustPositionAsAChildOfParentInMemory() {
    List<Note> siblings = getParent().children;
    siblings.remove(this);
    int insertIndex = 0;
    for (Note sibling : siblings) {
      if (sibling.getSiblingOrder() > getSiblingOrder()) {
        break;
      }
      insertIndex++;
    }
    siblings.add(insertIndex, this);
  }

  @JsonIgnore
  public boolean isRelation() {
    return getTargetNote() != null;
  }

  @JsonIgnore
  public String getNoteDescription() {
    String prettyString =
        new ObjectMapperConfig()
            .objectMapper()
            .valueToTree(BareNote.fromNoteWithoutTruncate(this))
            .toPrettyString();
    return """
        The %s (in JSON format):
        %s
        """
        .formatted(NOTE_OF_CURRENT_FOCUS, prettyString);
  }

  @JsonIgnore
  public String getUri() {
    return "/n" + getId();
  }

  public void initialize(User user, Note parentNote, Timestamp currentUTCTimestamp, String title) {
    setParentNote(parentNote);
    setUpdatedAt(currentUTCTimestamp);
    setTitle(title);
    setCreatedAt(currentUTCTimestamp);
    setUpdatedAt(currentUTCTimestamp);
    setCreator(user);
  }

  public void buildNotebookForHeadNote(Ownership ownership, User creator) {
    final Notebook notebook = new Notebook();
    notebook.setCreatorEntity(creator);
    notebook.setOwnership(ownership);
    notebook.setHeadNote(this);
    setNotebook(notebook);
  }

  @JsonIgnore
  public String getNotebookAssistantInstructions() {
    NotebookAiAssistant notebookAiAssistant = getNotebook().getNotebookAiAssistant();
    if (notebookAiAssistant == null) {
      return null;
    }
    return notebookAiAssistant.getAdditionalInstructionsToAi();
  }

  public NoteRealm toNoteRealm(User viewer) {
    NoteRealm nvb = new NoteRealm(this);
    nvb.setInboundReferences(
        getInboundReferences().stream().filter(l -> allowed(l, viewer)).toList());
    nvb.setFromBazaar(viewer == null || !viewer.owns(getNotebook()));

    return nvb;
  }

  private boolean allowed(Note l, User viewer) {
    if (l.getParent().getNotebook() == l.getTargetNote().getNotebook()) return true;
    if (viewer == null) return false;
    return viewer.canReferTo(l.getParent().getNotebook());
  }
}
