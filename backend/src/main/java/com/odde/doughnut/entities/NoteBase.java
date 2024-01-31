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
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;
import org.springframework.beans.BeanUtils;

@MappedSuperclass
@JsonPropertyOrder({"topic", "topicConstructor", "details", "parentId", "updatedAt"})
public abstract class NoteBase extends Thingy {

  @OneToOne(mappedBy = "note", cascade = CascadeType.ALL)
  @Getter
  @Setter
  @JsonIgnore
  private Thing thing;

  @ManyToOne
  @JoinColumn(name = "notebook_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Notebook notebook;

  @Embedded @Valid @Getter private final NoteAccessories noteAccessories = new NoteAccessories();

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

  @Column(name = "deleted_at")
  @Getter
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Timestamp deletedAt;

  @OneToMany(mappedBy = "sourceNote")
  @JsonIgnore
  private List<Link> links = new ArrayList<>();

  @OneToMany(mappedBy = "targetNote")
  @JsonIgnore
  private List<Link> refers = new ArrayList<>();

  @OneToMany(mappedBy = "parent", cascade = CascadeType.DETACH)
  @JsonIgnore
  @Where(clause = "deleted_at is null")
  @OrderBy("sibling_order")
  @Getter
  private final List<Note> allChildren = new ArrayList<>();

  @Column(name = "updated_at")
  @Getter
  @Setter
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
  @Setter
  private Note parent;

  @Embedded @JsonIgnore @Getter private ReviewSetting masterReviewSetting = new ReviewSetting();

  public void setDeletedAt(Timestamp value) {
    this.deletedAt = value;
    if (this.thing != null) this.thing.setDeletedAt(value);
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
  public List<Note> getChildren() {
    return getAllChildren().stream()
        .filter(nc -> !nc.usingLinkTypeAsTopicConstructor())
        .collect(toList());
  }

  @JsonIgnore
  public Link.LinkType getLinkType() {
    if (!getTopicConstructor().startsWith(":")) return null;
    return Link.LinkType.fromLabel(getTopicConstructor().substring(1));
  }

  public void setLinkType(Link.LinkType linkType) {
    setTopicConstructor(":" + linkType.label);
  }

  protected boolean usingLinkTypeAsTopicConstructor() {
    return getLinkType() != null;
  }

  protected String getLinkConstructor() {
    if (usingLinkTypeAsTopicConstructor()) {
      Link.LinkType linkType = getLinkType();
      if (linkType == null)
        throw new RuntimeException("Invalid link type: " + getTopicConstructor());
      return "%P is " + linkType.label + " %T";
    }
    return getTopicConstructor();
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

  @JsonIgnore
  public List<? extends Thingy> getLinkChildren() {
    //    return getAllChildren().stream()
    //        .filter(Note::usingLinkTypeAsTopicConstructor)
    //        .collect(toList());
    return getLinks();
  }

  @JsonIgnore
  public List<? extends Thingy> getLinks() {
    return links;
  }

  @JsonIgnore
  public List<? extends Thingy> getRefers() {
    return refers;
  }

  public String getTopic() {
    String constructor = getLinkConstructor();
    if (!constructor.contains("%P")) return constructor;
    Note parent = getParent();
    if (parent == null) return constructor;
    String target =
        getTargetNote() == null ? "missing target" : getTargetNote().getTopicConstructor();
    return constructor
        .replace("%P", "[" + parent.getTopicConstructor() + "]")
        .replace("%T", "[" + target + "]");
  }

  @Override
  public String toString() {
    return "Note{" + "id=" + id + ", title='" + getTopicConstructor() + '\'' + '}';
  }

  @JsonIgnore
  public void setParentNote(Note parentNote) {
    if (parentNote == null) return;
    setNotebook(parentNote.getNotebook());
    parent = parentNote;
  }

  public void mergeMasterReviewSetting(ReviewSetting reviewSetting) {
    BeanUtils.copyProperties(reviewSetting, getMasterReviewSetting());
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
    return getSiblings().stream().filter(nc -> nc.getSiblingOrder() > siblingOrder).findFirst();
  }

  private long getSiblingOrderToInsertBehindMe() {
    Optional<Note> nextSiblingNote = nextSibling();
    return nextSiblingNote
        .map(x -> (siblingOrder + x.getSiblingOrder()) / 2)
        .orElse(siblingOrder + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT);
  }

  private Long getSiblingOrderToBecomeMyFirstChild() {
    Note firstChild = getFirstChild();
    if (firstChild != null) {
      return firstChild.getSiblingOrder() - SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
    }
    return null;
  }

  protected Long theSiblingOrderItTakesToMoveRelativeToMe(boolean asFirstChildOfNote) {
    if (!asFirstChildOfNote) {
      return getSiblingOrderToInsertBehindMe();
    }
    return getSiblingOrderToBecomeMyFirstChild();
  }

  public Optional<Integer> getParentId() {
    Note parent = getParent();
    if (parent == null) return Optional.empty();
    return Optional.ofNullable(parent.id);
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

  protected Optional<String> getNotePicture() {
    if (getNoteAccessories().getUseParentPicture() && getParent() != null) {
      return getParent().getNotePicture();
    }
    return getNoteAccessories().getNotePicture();
  }

  public void prependDescription(String addition) {
    String prevDesc = getDetails() != null ? getDetails() : "";
    String desc = prevDesc.isEmpty() ? addition : addition + "\n" + prevDesc;
    setDetails(desc);
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

  @JsonIgnore
  public Stream<Note> getDescendants() {
    return getAllChildren().stream().flatMap(c -> Stream.concat(Stream.of(c), c.getDescendants()));
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
