package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.algorithms.ClozeDescription;
import com.odde.doughnut.algorithms.SiblingOrder;
import com.odde.doughnut.entities.validators.ValidateNotePicture;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Entity
@Table(name = "note")
@ValidateNotePicture
public class NoteEntity {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @NotNull @Size(min = 1, max = 100) @Getter @Setter private String title;
  @Getter @Setter private String description;
  @Getter @Setter private String picture;
  @Getter @Setter private String url;

  @Column(name = "url_is_video")
  @Getter
  @Setter
  private Boolean urlIsVideo = false;

  @Column(name = "skip_review")
  @Getter
  @Setter
  private Boolean skipReview = false;

  @Column(name = "use_parent_picture")
  @Getter
  @Setter
  private Boolean useParentPicture = false;

  @Pattern(regexp="^(((-?[0-9.]+\\s+){3}-?[0-9.]+\\s+)*((-?[0-9.]+\\s+){3}-?[0-9.]+))?$",message="must be 'x y width height [x y width height...]'")
  @Column(name = "picture_mask")
  @Getter
  @Setter
  private String pictureMask;

  @Column(name = "sibling_order")
  @Getter
  @Setter
  private Long siblingOrder = SiblingOrder.getGoodEnoughOrderNumber();

  @Override
  public String toString() {
    return "Note{"
        + "id=" + id + ", title='" + title + '\'' + '}';
  }

  @ManyToOne
  @JoinColumn(name = "parent_id")
  @JsonIgnore
  private NoteEntity parentNote;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "ownership_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private OwnershipEntity ownershipEntity;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "master_review_setting_id", referencedColumnName = "id")
  @Getter
  @Setter
  private ReviewSettingEntity masterReviewSettingEntity;

  @OneToMany(mappedBy = "parentNote", cascade = CascadeType.ALL,
             orphanRemoval = true)
  @OrderBy("sibling_order")
  @JsonIgnore
  @Getter
  private final List<NoteEntity> children = new ArrayList<>();

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private UserEntity userEntity;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "image_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private ImageEntity uploadPicture;

  @Column(name = "created_datetime")
  @Getter
  @Setter
  private Timestamp createdDatetime = new Timestamp(System.currentTimeMillis());

  @Column(name = "updated_datetime")
  @Getter
  @Setter
  private Timestamp updatedDatetime = new Timestamp(System.currentTimeMillis());

  @OneToMany(mappedBy = "sourceNote", cascade = CascadeType.ALL,
             orphanRemoval = true)
  @JsonIgnore
  @Getter
  @Setter
  private List<LinkEntity> links = new ArrayList<>();

  @OneToMany(mappedBy = "targetNote", cascade = CascadeType.ALL,
             orphanRemoval = true)
  @JsonIgnore
  @Getter
  @Setter
  private List<LinkEntity> refers = new ArrayList<>();

  @OneToMany(mappedBy = "noteEntity", cascade = CascadeType.ALL)
  @JsonIgnore
  @OrderBy("depth DESC")
  @Getter
  @Setter
  private List<NotesClosureEntity> notesClosures = new ArrayList<>();

  @OneToMany(mappedBy = "ancestorEntity", cascade = CascadeType.DETACH)
  @JsonIgnore
  @OrderBy("depth")
  @Getter
  @Setter
  private List<NotesClosureEntity> descendantNCs = new ArrayList<>();

  @Transient @Getter @Setter private String testingParent;
  @Transient @Getter @Setter private MultipartFile uploadPictureProxy;

  public List<NoteEntity> getTargetNotes() {
    return links.stream().map(LinkEntity::getTargetNote).collect(toList());
  }

  public List<LinkEntity.LinkType> linkTypes() {
    return Arrays.stream(LinkEntity.LinkType.values())
        .filter(t -> !linkedNotesOfType(t).isEmpty())
        .collect(Collectors.toUnmodifiableList());
  }

  public List<LinkEntity> linksOfTypeThroughDirect(LinkEntity.LinkType linkType) {
    return this.links.stream()
            .filter(l -> l.getLinkType().equals(linkType))
            .collect(Collectors.toList());
  }

  public List<LinkEntity> linksOfTypeThroughReverse(LinkEntity.LinkType linkType) {
    return refers.stream()
                    .filter(l -> l.getLinkType().equals(linkType.reverseType()))
                    .collect(Collectors.toUnmodifiableList());
  }

  public List<NoteEntity> linkedNotesOfType(LinkEntity.LinkType linkType) {
      List<NoteEntity> notes = new ArrayList<>();
      linksOfTypeThroughDirect(linkType).forEach(lk->notes.add(lk.getTargetNote()));
      linksOfTypeThroughReverse(linkType).forEach(lk->notes.add(lk.getSourceNote()));
      return notes;
  }

  public boolean isFromCircle() { return circle() != null; }

  public CircleEntity circle() { return ownershipEntity.getCircleEntity(); }

  public String getClozeDescription() {
    return new ClozeDescription().getClozeDescription(this.title,
                                                      this.description);
  }

  public String getNotePicture() {
    if (useParentPicture && parentNote != null) {
        return parentNote.getNotePicture();
    }

    if (uploadPicture != null) {
      return "/images/" + uploadPicture.getId() + "/" + uploadPicture.getName();
    }

    return picture;
  }

  public String getPictureMaskSvg(String opacity) {
    if(Strings.isEmpty(pictureMask)) {
      return "";
    }
    List<String> list = Arrays.stream(pictureMask.split("\\s+")).collect(Collectors.toUnmodifiableList());
    List<String> results = new ArrayList<>();
    for (int i = 0; i < list.size(); i+=4) {
      results.add(String.format("<rect x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\" style=\"fill:blue;stroke:pink;stroke-width:1;fill-opacity:%s;stroke-opacity:0.8\" />", list.get(i), list.get(i+1), list.get(i+2), list.get(i+3), opacity));
    }

    return String.join("", results);
  }

  public boolean hasPicture() {
    return Strings.isNotBlank(picture) || uploadPicture != null || useParentPicture;
  }

  public boolean isHead() {
    return parentNote == null;
  }

  public void addAncestors(List<NoteEntity> ancestors) {
      int[] counter = {1};
      ancestors.forEach(anc -> {
          NotesClosureEntity notesClosureEntity = new NotesClosureEntity();
          notesClosureEntity.setNoteEntity(this);
          notesClosureEntity.setAncestorEntity(anc);
          notesClosureEntity.setDepth(counter[0]);
          getNotesClosures().add(0, notesClosureEntity);
          counter[0] += 1;
      });
  }

  public void setParentNote(NoteEntity parentNote) {
    this.parentNote = parentNote;
    if (parentNote == null) return;
    List<NoteEntity> ancestorsIncludingMe = parentNote.getAncestorsIncludingMe();
    Collections.reverse(ancestorsIncludingMe);
    addAncestors(ancestorsIncludingMe);
  }

  public List<NoteEntity> getAncestorsIncludingMe() {
    List<NoteEntity> ancestors = getAncestors();
    ancestors.add(this);
      return ancestors;
  }

  private List<NoteEntity> getAncestors() {
    return getNotesClosures().stream().map(NotesClosureEntity::getAncestorEntity).collect(toList());
  }

  public void traverseBreadthFirst(Consumer<NoteEntity> noteEntityConsumer) {
    descendantNCs.stream().map(NotesClosureEntity::getNoteEntity).forEach(noteEntityConsumer);
  }

  public NoteEntity getParentNote() {
    List<NoteEntity> ancestors = getAncestors();
    if (ancestors.size() == 0) {
      return null;
    }
    return ancestors.get(ancestors.size() - 1);
  }
}
