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
import com.odde.doughnut.controllers.dto.NoteAccessoriesDTO;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionFactory;
import com.odde.doughnut.models.NoteViewer;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;
import org.springframework.web.multipart.MultipartFile;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "note")
@JsonPropertyOrder({"topic", "topicConstructor", "details", "parentId", "updatedAt"})
public abstract class Note extends EntityIdentifiedByIdOnly {
  public static final int MAX_TITLE_LENGTH = 150;

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
  @Setter
  private Notebook notebook;

  @NotNull @Embedded @Valid @Getter
  private final NoteAccessories noteAccessories = new NoteAccessories();

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
  private List<LinkingNote> refers = new ArrayList<>();

  @OneToMany(mappedBy = "parent", cascade = CascadeType.DETACH)
  @OrderBy("siblingOrder")
  private final List<LinkingNote> links = new ArrayList<>();

  @OneToMany(mappedBy = "parent", cascade = CascadeType.DETACH)
  @JsonIgnore
  @OrderBy("siblingOrder")
  private final List<HierarchicalNote> children = new ArrayList<>();

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

  @Embedded @JsonIgnore @Getter private ReviewSetting reviewSetting = new ReviewSetting();

  @JsonIgnore
  public List<HierarchicalNote> getChildren() {
    return filterDeleted(children);
  }

  @JsonIgnore
  public List<LinkingNote> getLinks() {
    return filterDeleted(links);
  }

  @JsonIgnore
  public List<LinkingNote> getRefers() {
    return filterDeleted(refers);
  }

  private static <T extends Note> List<T> filterDeleted(List<T> notes) {
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
  public List<HierarchicalNote> getSiblings() {
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

  public void setLinkType(LinkType linkType) {
    setTopicConstructor(":" + linkType.label);
  }

  protected String getLinkConstructor() {
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

  @NotNull
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

  public void setFromDTO(NoteAccessoriesDTO noteAccessoriesDTO, User user) throws IOException {
    BeanUtils.copyProperties(noteAccessoriesDTO, getNoteAccessories());
    Image uploadPicture = noteAccessoriesDTO.fetchUploadedPicture(user);
    if (uploadPicture != null) {
      getNoteAccessories().setUploadPicture(uploadPicture);
    }
  }

  public void setAudio(MultipartFile file, User user) throws IOException {

    Audio audio = new Audio();
    audio.setUser(user);
    audio.setStorageType("db");
    audio.setName(file.getOriginalFilename());
    audio.setType(file.getContentType());

    AttachmentBlob audioBlob = new AttachmentBlob();
    audioBlob.setData(file.getBytes());
    audio.setBlob(audioBlob);

    getNoteAccessories().setUploadAudio(audio);
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

  private Optional<HierarchicalNote> nextSibling() {
    return getSiblings().stream().filter(nc -> nc.getSiblingOrder() > siblingOrder).findFirst();
  }

  private long getSiblingOrderToInsertBehindMe() {
    Optional<HierarchicalNote> nextSiblingNote = nextSibling();
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
        .map(Note::getTopicConstructor)
        .collect(Collectors.joining(" › "));
  }

  @JsonIgnore
  public boolean matchAnswer(String spellingAnswer) {
    return getNoteTitle().matches(spellingAnswer);
  }

  @JsonIgnore
  public Stream<Note> getDescendants() {
    return getChildren().stream().flatMap(c -> Stream.concat(Stream.of(c), c.getDescendants()));
  }

  @JsonIgnore
  public Stream<Note> getLinksAndRefers() {
    return Stream.concat(getLinks().stream(), getRefers().stream());
  }

  @JsonIgnore
  public abstract List<QuizQuestionFactory> getQuizQuestionFactories();

  public static class NoteBrief {
    public String contextPath;
    public String topic;
    public String details;
  }

  @JsonIgnore
  public String getNoteDescription() {
    Note.NoteBrief noteBrief = new Note.NoteBrief();
    noteBrief.contextPath = getContextPathString();
    noteBrief.topic = getTopicConstructor();
    noteBrief.details = getDetails();
    return """
The note of current focus (in JSON format):
%s
"""
        .formatted(defaultObjectMapper().valueToTree(noteBrief).toPrettyString());
  }

  protected void initialize(
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

  public Thing buildNoteThing() {
    Thing result = new Thing();
    result.setNote(this);
    return result;
  }

  @Transient @Getter @Setter public String srt;
}
