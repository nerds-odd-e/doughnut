package com.odde.doughnut.entities;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.algorithms.HtmlOrMarkdown;
import com.odde.doughnut.algorithms.NoteTitle;
import com.odde.doughnut.algorithms.SiblingOrder;
import com.odde.doughnut.controllers.dto.NoteTopic;
import com.odde.doughnut.models.NoteViewer;
import com.odde.doughnut.models.TimestampOperations;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "note")
@JsonPropertyOrder({"topic", "noteTopic", "details", "parentId", "linkType", "updatedAt"})
public class Note extends EntityIdentifiedByIdOnly {
  public static final int MAX_TITLE_LENGTH = 150;
  private static final String PATH_DELIMITER = " › ";

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

  @Column(name = "description")
  @Getter
  @Setter
  @JsonPropertyDescription("The details of the note is in markdown format.")
  private String details;

  @Size(min = 1, max = Note.MAX_TITLE_LENGTH)
  @Getter
  @Setter
  @Column(name = "topic_constructor")
  @NotNull
  @JsonIgnore
  private String topicConstructor = "";

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
  private List<Note> refers = new ArrayList<>();

  @OneToMany(mappedBy = "parent", cascade = CascadeType.DETACH)
  @OrderBy("siblingOrder")
  private final List<Note> links = new ArrayList<>();

  @OneToMany(mappedBy = "parent", cascade = CascadeType.DETACH)
  @JsonIgnore
  @OrderBy("siblingOrder")
  private final List<Note> children = new ArrayList<>();

  @OneToMany(mappedBy = "note")
  @JsonIgnore
  private Set<ReviewPoint> reviewPoints;

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

  @OneToMany(mappedBy = "note")
  @Getter
  @JsonIgnore
  private List<PredefinedQuestion> predefinedQuestions = new ArrayList<>();

  @Embedded @JsonIgnore @Getter private ReviewSetting reviewSetting = new ReviewSetting();

  @JsonIgnore
  public List<Note> getChildren() {
    return filterDeletedUnmodifiableNoteList(children);
  }

  @JsonIgnore
  public List<Note> getLinks() {
    return filterDeletedUnmodifiableNoteList(links);
  }

  @JsonIgnore
  public List<Note> getRefers() {
    return filterDeletedUnmodifiableNoteList(refers);
  }

  public static <T extends Note> List<T> filterDeletedUnmodifiableNoteList(List<T> notes) {
    return notes.stream().filter(n -> n.getDeletedAt() == null).toList();
  }

  @JsonIgnore
  public boolean targetVisibleAsSourceOrTo(User viewer) {
    if (getParent().getNotebook() == getTargetNote().getNotebook()) return true;
    if (viewer == null) return false;
    return viewer.canReferTo(getTargetNote().getNotebook());
  }

  @JsonIgnore
  public NoteTitle getNoteTitle() {
    return new NoteTitle(getTopicConstructor());
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
  public LinkType getLinkType() {
    if (!getTopicConstructor().startsWith(":")) return null;
    return LinkType.fromLabel(getTopicConstructor().substring(1));
  }

  @JsonIgnore
  public void setLinkType(LinkType linkType) {
    setTopicConstructor(":" + linkType.label);
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
    return "Note{" + "id=" + id + ", title='" + getTopicConstructor() + '\'' + '}';
  }

  @JsonIgnore
  public void setParentNote(Note parentNote) {
    if (parentNote == null) return;
    setNotebook(parentNote.getNotebook());
    this.parent = parentNote;
    // Update notebook for all descendants including links
    getAllDescendants().forEach(descendant -> descendant.setNotebook(parentNote.getNotebook()));
  }

  private void setNotebook(Notebook notebook) {
    this.notebook = notebook;
  }

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
  public String getContextPathString() {
    return getAncestors().stream()
        .map(Note::getTopicConstructor)
        .collect(Collectors.joining(PATH_DELIMITER));
  }

  @JsonIgnore
  public boolean matchAnswer(String spellingAnswer) {
    return getNoteTitle().matches(spellingAnswer);
  }

  @JsonIgnore
  private List<Note> getNoneLinkChildren() {
    return getChildren().stream().filter(c -> c.getLinkType() == null).toList();
  }

  @JsonIgnore
  public List<Note> getNoneLinkSiblings() {
    if (getParent() == null) {
      return new ArrayList<>();
    }
    return getParent().getNoneLinkChildren();
  }

  @JsonIgnore
  public Stream<Note> getAllDescendants() {
    return Stream.concat(
        getChildren().stream(), getChildren().stream().flatMap(Note::getAllDescendants));
  }

  @JsonIgnore
  public Stream<Note> getAllNoneLinkDescendants() {
    return getAllDescendants().filter(n -> n.getLinkType() == null);
  }

  @JsonIgnore
  public Stream<Note> getLinksAndRefers() {
    return Stream.concat(getLinks().stream(), getRefers().stream());
  }

  @JsonIgnore
  public NoteAccessory getOrInitializeNoteAccessory() {
    if (noteAccessory == null) {
      noteAccessory = new NoteAccessory();
      noteAccessory.setNote(this);
    }
    return noteAccessory;
  }

  @org.springframework.lang.NonNull
  public NoteTopic getNoteTopic() {
    NoteTopic noteTopic = new NoteTopic();
    noteTopic.setId(getId());
    noteTopic.setTopicConstructor(getTopicConstructor());
    noteTopic.setShortDetails(new HtmlOrMarkdown(getDetails()).beginning(50));
    noteTopic.setLinkType(getLinkType());
    if (getParent() != null) {
      noteTopic.setParentNoteTopic(getParent().getNoteTopic());
    }
    if (getTargetNote() != null) {
      noteTopic.setTargetNoteTopic(getTargetNote().getNoteTopic());
    }
    return noteTopic;
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

  public static class NoteBrief {
    public String uri;
    public String contextPath;
    public String topic;
    public String details;
    public String createdAt;
    public String target;
  }

  @JsonIgnore
  public String getNoteDescription() {
    String prettyString = defaultObjectMapper().valueToTree(getNoteBrief()).toPrettyString();
    return """
        The note of current focus (in JSON format):
        %s
        """
        .formatted(prettyString);
  }

  @JsonIgnore
  public NoteBrief getNoteBrief() {
    NoteBrief noteBrief = new NoteBrief();
    noteBrief.uri = "https://doughnut.odd-e.com/n" + getId();
    noteBrief.contextPath = getContextPathString();
    noteBrief.topic = getTopicConstructor();
    noteBrief.details = getDetails();
    noteBrief.createdAt =
        TimestampOperations.getZonedDateTime(getCreatedAt(), ZoneId.systemDefault()).toString();
    if (targetNote != null) {
      noteBrief.target =
          targetNote.getContextPathString() + PATH_DELIMITER + targetNote.getTopicConstructor();
    }
    return noteBrief;
  }

  public void initialize(
      User user, Note parentNote, Timestamp currentUTCTimestamp, String topicConstructor) {
    setParentNote(parentNote);
    setUpdatedAt(currentUTCTimestamp);
    setTopicConstructor(topicConstructor);
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

  public NoteViewer targetNoteViewer(User user) {
    return new NoteViewer(user, getTargetNote());
  }

  @JsonIgnore
  public Stream<Note> getSiblingLinksOfSameLinkType(User user) {
    return targetNoteViewer(user)
        .linksOfTypeThroughReverse(getLinkType())
        .filter(l -> !l.equals(this));
  }

  @JsonIgnore
  public List<Note> getLinkedSiblingsOfSameLinkType(User user) {
    return getSiblingLinksOfSameLinkType(user).map(Note::getParent).toList();
  }
}
