package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.algorithms.ClozedString;
import com.odde.doughnut.models.NoteViewer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "thing")
@JsonPropertyOrder({"note", "link", "linkType", "sourceNote", "targetNote", "createdAt"})
public class Thing extends EntityIdentifiedByIdOnly {

  @Column(name = "created_at")
  @Setter
  @Getter
  private Timestamp createdAt;

  @Column(name = "deleted_at")
  @JsonIgnore
  @Setter
  private Timestamp deletedAt;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  @Setter
  @Nullable
  private Note note;

  @OneToOne
  @JoinColumn(name = "creator_id")
  @JsonIgnore
  @Getter
  @Setter
  private User creator;

  public static Note createThing(User user, Note note, Timestamp currentUTCTimestamp) {
    final Thing thing = new Thing();
    thing.setNote(note);
    thing.setCreator(user);
    thing.setCreatedAt(currentUTCTimestamp);
    note.setThing(thing);
    return note;
  }

  @JsonIgnore
  public ClozedString getClozeSource() {
    NoteBase source = getParentNote();
    NoteBase target = getTargetNote();
    return ClozedString.htmlClozedString(source.getTopicConstructor()).hide(target.getNoteTitle());
  }

  @JsonIgnore
  public Note getParentNote() {
    return getNote().getParent();
  }

  public void setSourceNote(Note from) {
    getNote().setParentNote(from);
  }

  @Nullable
  public Note getSourceNote() {
    return getParentNote();
  }

  @Nullable
  public Note getTargetNote() {
    return getNote().getTargetNote();
  }

  public void setTargetNote(Note to) {
    getNote().setTargetNote(to);
  }

  @JsonIgnore
  public String getLinkTypeLabel() {
    return getLinkType().label;
  }

  @Nullable
  public LinkType getLinkType() {
    return getNote().getLinkType();
  }

  public void setLinkType(LinkType linkType) {
    getNote().setLinkType(linkType);
  }

  @JsonIgnore
  public Stream<Thing> getSiblingLinksOfSameLinkType(User user) {
    return new NoteViewer(user, getTargetNote())
        .linksOfTypeThroughReverse(getLinkType())
        .filter(l -> !l.equals(this));
  }

  @JsonIgnore
  public List<Note> getLinkedSiblingsOfSameLinkType(User user) {
    return getSiblingLinksOfSameLinkType(user).map(Thing::getParentNote).toList();
  }

  @JsonIgnore
  public List<Thing> categoryLinksOfTarget(User user) {
    return new NoteViewer(user, getTargetNote())
        .linksOfTypeThroughDirect(
            List.of(LinkType.PART, LinkType.INSTANCE, LinkType.SPECIALIZE, LinkType.APPLICATION));
  }

  @JsonIgnore
  public boolean sourceVisibleAsTargetOrTo(User viewer) {
    if (getSourceNote().getNotebook() == getTargetNote().getNotebook()) return true;
    if (viewer == null) return false;

    return viewer.canReferTo(getSourceNote().getNotebook());
  }

  @JsonIgnore
  public Integer getLevel() {
    return getNote().getReviewSetting().getLevel();
  }

  @JsonIgnore
  public void setLevelIfHigher(Integer level) {
    getNote().getReviewSetting().setLevel(Math.max(getLevel(), level));
  }
}
