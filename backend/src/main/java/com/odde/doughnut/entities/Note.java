package com.odde.doughnut.entities;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.algorithms.HtmlOrMarkdown;
import com.odde.doughnut.algorithms.NoteTitle;
import com.odde.doughnut.algorithms.SiblingOrder;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.WhereJoinTable;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name = "note")
@JsonPropertyOrder({"topic", "topicConstructor", "details", "parentId", "updatedAt"})
public class Note extends Thingy {
  public static final int MAX_TITLE_LENGTH = 150;

  private Note() {}

  @Embedded @Valid @Getter private final NoteAccessories noteAccessories = new NoteAccessories();

  @OneToOne(mappedBy = "note", cascade = CascadeType.ALL)
  @Getter
  @Setter
  @JsonIgnore
  private Thing thing;

  @Column(name = "description")
  @Getter
  @Setter
  @JsonPropertyDescription("The details of the note is in markdown format.")
  private String details;

  @Size(min = 1, max = Note.MAX_TITLE_LENGTH)
  @Getter
  @Setter
  @Column(name = "topic_constructor")
  private String topicConstructor = "";

  @Column(name = "updated_at")
  @Getter
  @Setter
  private Timestamp updatedAt;

  @Column(name = "wikidata_id")
  @Getter
  @Setter
  private String wikidataId;

  @Column(name = "sibling_order")
  private Long siblingOrder = SiblingOrder.getGoodEnoughOrderNumber();

  @ManyToOne
  @JoinColumn(name = "notebook_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  private Notebook notebook;

  @ManyToOne
  @JoinColumn(name = "target_note_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Note targetNote;

  @Column(name = "deleted_at")
  @Getter
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Timestamp deletedAt;

  public void setDeletedAt(Timestamp value) {
    this.deletedAt = value;
    if (this.thing != null) this.thing.setDeletedAt(value);
  }

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "master_review_setting_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private ReviewSetting masterReviewSetting;

  @OneToMany(mappedBy = "sourceNote")
  @JsonIgnore
  @Getter
  @Setter
  private List<Link> links = new ArrayList<>();

  @OneToMany(mappedBy = "targetNote")
  @JsonIgnore
  @Getter
  @Setter
  private List<Link> refers = new ArrayList<>();

  @OneToMany(mappedBy = "note", cascade = CascadeType.ALL)
  @JsonIgnore
  @OrderBy("depth DESC")
  @Getter
  @Setter
  private List<NotesClosure> ancestorNotesClosures = new ArrayList<>();

  @JoinTable(
      name = "notes_closure",
      joinColumns = {
        @JoinColumn(
            name = "ancestor_id",
            referencedColumnName = "id",
            nullable = false,
            insertable = false,
            updatable = false)
      },
      inverseJoinColumns = {
        @JoinColumn(
            name = "note_id",
            referencedColumnName = "id",
            nullable = false,
            insertable = false,
            updatable = false)
      })
  @OneToMany(cascade = CascadeType.DETACH)
  @JsonIgnore
  @WhereJoinTable(clause = "depth = 1")
  @Where(clause = "deleted_at is null")
  @OrderBy("sibling_order")
  @Getter
  private final List<Note> allChildren = new ArrayList<>();

  public static Note createNote(User user, Timestamp currentUTCTimestamp, String topicConstructor) {
    final Note note = new Note();
    note.setUpdatedAt(currentUTCTimestamp);
    note.setTopicConstructor(topicConstructor);
    note.setUpdatedAt(currentUTCTimestamp);

    Thing.createThing(user, note, currentUTCTimestamp);
    return note;
  }

  @JsonIgnore
  public List<Note> getChildren() {
    return getAllChildren().stream()
        .filter(nc -> !nc.usingLinkTypeAsTopicConstructor())
        .collect(toList());
  }

  @JsonIgnore
  public List<Note> getLinkChildren() {
    return getAllChildren().stream()
        .filter(Note::usingLinkTypeAsTopicConstructor)
        .collect(toList());
  }

  public String getTopic() {
    String constructor = getLinkConstructor();
    if (!constructor.contains("%P")) return constructor;
    Note parent = getParentNote();
    if (parent == null) return constructor;
    String target = getTargetNote() == null ? "missing target" : getTargetNote().getTopic();
    return constructor
        .replace("%P", "[" + parent.getTopic() + "]")
        .replace("%T", "[" + target + "]");
  }

  private String getLinkConstructor() {
    if (usingLinkTypeAsTopicConstructor()) {
      Link.LinkType linkType = Link.LinkType.fromLabel(topicConstructor.substring(1));
      if (linkType == null) throw new RuntimeException("Invalid link type: " + topicConstructor);
      return "%P is " + linkType.label + " %T";
    }
    return topicConstructor;
  }

  private boolean usingLinkTypeAsTopicConstructor() {
    return topicConstructor.startsWith(":");
  }

  @Override
  public String toString() {
    return "Note{" + "id=" + id + ", title='" + getTopicConstructor() + '\'' + '}';
  }

  private void addAncestors(List<Note> ancestors) {
    int[] counter = {1};
    ancestors.forEach(
        anc -> {
          NotesClosure notesClosure = new NotesClosure();
          notesClosure.setNote(this);
          notesClosure.setAncestor(anc);
          notesClosure.setDepth(counter[0]);
          getAncestorNotesClosures().add(0, notesClosure);
          counter[0] += 1;
        });
  }

  @JsonIgnore
  public void setParentNote(Note parentNote) {
    if (parentNote == null) return;
    notebook = parentNote.getNotebook();
    List<Note> ancestors = parentNote.getAncestors();
    ancestors.add(parentNote);
    Collections.reverse(ancestors);
    addAncestors(ancestors);
  }

  @JsonIgnore
  public List<Note> getAncestors() {
    return getAncestorNotesClosures().stream().map(NotesClosure::getAncestor).collect(toList());
  }

  @JsonIgnore
  public Note getParentNote() {
    try {
      return getAncestors().getLast();
    } catch (NoSuchElementException e) {
      return null;
    }
  }

  @JsonIgnore
  public List<Note> getSiblings() {
    if (getParentNote() == null) {
      return new ArrayList<>();
    }
    return Collections.unmodifiableList(getParentNote().getChildren());
  }

  public void mergeMasterReviewSetting(ReviewSetting reviewSetting) {
    ReviewSetting current = getMasterReviewSetting();
    if (current == null) {
      setMasterReviewSetting(reviewSetting);
    } else {
      BeanUtils.copyProperties(reviewSetting, getMasterReviewSetting());
    }
  }

  public void updateNoteContent(NoteAccessories noteAccessories, User user) throws IOException {
    if (noteAccessories.getUploadPicture() == null) {
      noteAccessories.setUploadPicture(getNoteAccessories().getUploadPicture());
    }
    BeanUtils.copyProperties(noteAccessories, getNoteAccessories());
  }

  @JsonIgnore
  private Note getFirstChild() {
    return getChildren().stream().findFirst().orElse(null);
  }

  public void updateSiblingOrder(Note relativeToNote, boolean asFirstChildOfNote) {
    Long newSiblingOrder =
        relativeToNote.theSiblingOrderItTakesToMoveRelativeToMe(asFirstChildOfNote);
    if (newSiblingOrder != null) {
      siblingOrder = newSiblingOrder;
    }
  }

  private Optional<Note> nextSibling() {
    return getSiblings().stream().filter(nc -> nc.siblingOrder > siblingOrder).findFirst();
  }

  private long getSiblingOrderToInsertBehindMe() {
    Optional<Note> nextSiblingNote = nextSibling();
    return nextSiblingNote
        .map(x -> (siblingOrder + x.siblingOrder) / 2)
        .orElse(siblingOrder + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT);
  }

  private Long getSiblingOrderToBecomeMyFirstChild() {
    Note firstChild = getFirstChild();
    if (firstChild != null) {
      return firstChild.siblingOrder - SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
    }
    return null;
  }

  private Long theSiblingOrderItTakesToMoveRelativeToMe(boolean asFirstChildOfNote) {
    if (!asFirstChildOfNote) {
      return getSiblingOrderToInsertBehindMe();
    }
    return getSiblingOrderToBecomeMyFirstChild();
  }

  public void buildNotebookForHeadNote(Ownership ownership, User creator) {
    final Notebook notebook = new Notebook();
    notebook.setCreatorEntity(creator);
    notebook.setOwnership(ownership);
    notebook.setHeadNote(this);

    this.notebook = notebook;
  }

  public Optional<Integer> getParentId() {
    Note parent = getParentNote();
    if (parent == null) return Optional.empty();
    return Optional.ofNullable(parent.id);
  }

  @JsonIgnore
  public Note getGrandAsPossible() {
    Note grand = this;
    for (int i = 0; i < 2; i++) if (grand.getParentNote() != null) grand = grand.getParentNote();
    return grand;
  }

  @JsonIgnore
  public ClozedString getClozeDescription() {
    if (isDetailsBlankHtml()) return new ClozedString(null, "");

    return ClozedString.htmlClozedString(getDetails()).hide(getNoteTitle());
  }

  @JsonIgnore
  public NoteTitle getNoteTitle() {
    return new NoteTitle(getTopicConstructor());
  }

  public Optional<PictureWithMask> getPictureWithMask() {
    return getNotePicture()
        .map(
            (pic) -> {
              PictureWithMask pictureWithMask = new PictureWithMask();
              pictureWithMask.notePicture = pic;
              pictureWithMask.pictureMask = getNoteAccessories().getPictureMask();
              return pictureWithMask;
            });
  }

  private Optional<String> getNotePicture() {
    if (noteAccessories.getUseParentPicture() && getParentNote() != null) {
      return getParentNote().getNotePicture();
    }
    return noteAccessories.getNotePicture();
  }

  @JsonIgnore
  public Integer getLevel() {
    if (masterReviewSetting == null) return ReviewSetting.defaultLevel;
    return masterReviewSetting.getLevel();
  }

  public void prependDescription(String addition) {
    String prevDesc = getDetails() != null ? getDetails() : "";
    String desc = prevDesc.isEmpty() ? addition : addition + "\n" + prevDesc;
    setDetails(desc);
  }

  public Link buildLinkToParent(
      User user, Link.LinkType linkTypeToParent, Timestamp currentUTCTimestamp) {
    return buildLinkToNote(user, linkTypeToParent, currentUTCTimestamp, getParentNote());
  }

  public Link buildLinkToNote(
      User user, Link.LinkType linkType, Timestamp currentUTCTimestamp, Note targetNote) {
    if (linkType == null || linkType == Link.LinkType.NO_LINK) {
      return null;
    }
    Link link = Link.createLink(this, targetNote, user, linkType, currentUTCTimestamp);
    getRefers().add(link);
    return link;
  }

  public Note buildChildNote(User user, Timestamp currentUTCTimestamp, String topicConstructor) {
    Note note = createNote(user, currentUTCTimestamp, topicConstructor);
    note.setParentNote(this);
    return note;
  }

  @JsonIgnore
  public boolean isDetailsBlankHtml() {
    return new HtmlOrMarkdown(getDetails()).isBlank();
  }

  @JsonIgnore
  public String getContextPathString() {
    return getAncestors().stream()
        .map(note -> note.getTopicConstructor())
        .collect(Collectors.joining(" › "));
  }

  @JsonIgnore
  public boolean matchAnswer(String spellingAnswer) {
    return getNoteTitle().matches(spellingAnswer);
  }

  public static class NoteBrief {
    public String contextPath;
    public String topic;
    public String details;
  }

  @JsonIgnore
  public String getNoteDescription() {
    NoteBrief noteBrief = new NoteBrief();
    noteBrief.contextPath = getContextPathString();
    noteBrief.topic = getTopicConstructor();
    noteBrief.details = getDetails();
    return """
The note of current focus (in JSON format):
%s
"""
        .formatted(defaultObjectMapper().valueToTree(noteBrief).toPrettyString());
  }

  @Override
  public void beforeCreate(ModelFactoryService modelFactoryService) {}
}
