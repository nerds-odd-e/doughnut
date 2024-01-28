package com.odde.doughnut.entities;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.algorithms.HtmlOrMarkdown;
import com.odde.doughnut.algorithms.SiblingOrder;
import jakarta.persistence.*;
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
public class Note extends Notey {
  public static final int MAX_TITLE_LENGTH = 150;

  private Note() {}

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
  @JoinColumn(name = "target_note_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private NoteSimple targetNote;

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
  public List<? extends Thingy> getLinkChildren() {
    //    return getAllChildren().stream()
    //        .filter(Note::usingLinkTypeAsTopicConstructor)
    //        .collect(toList());
    return getLinks();
  }

  public String getTopic() {
    String constructor = getLinkConstructor();
    if (!constructor.contains("%P")) return constructor;
    Note parent = getParentNote();
    if (parent == null) return constructor;
    String target =
        getTargetNote() == null ? "missing target" : getTargetNote().getTopicConstructor();
    return constructor
        .replace("%P", "[" + parent.getTopicConstructor() + "]")
        .replace("%T", "[" + target + "]");
  }

  private String getLinkConstructor() {
    if (usingLinkTypeAsTopicConstructor()) {
      Link.LinkType linkType = getLinkType();
      if (linkType == null)
        throw new RuntimeException("Invalid link type: " + getTopicConstructor());
      return "%P is " + linkType.label + " %T";
    }
    return getTopicConstructor();
  }

  @JsonIgnore
  public Link.LinkType getLinkType() {
    if (!getTopicConstructor().startsWith(":")) return null;
    return Link.LinkType.fromLabel(getTopicConstructor().substring(1));
  }

  private boolean usingLinkTypeAsTopicConstructor() {
    return getLinkType() != null;
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
    setNotebook(parentNote.getNotebook());
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
    return getParentNote().getChildren();
  }

  public void mergeMasterReviewSetting(ReviewSetting reviewSetting) {
    ReviewSetting current = getMasterReviewSetting();
    if (current == null) {
      setMasterReviewSetting(reviewSetting);
    } else {
      BeanUtils.copyProperties(reviewSetting, getMasterReviewSetting());
    }
  }

  public void updateNoteContent(NoteAccessories noteAccessories) {
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

    setNotebook(notebook);
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
    if (getNoteAccessories().getUseParentPicture() && getParentNote() != null) {
      return getParentNote().getNotePicture();
    }
    return getNoteAccessories().getNotePicture();
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
}
