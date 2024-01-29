package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "link_id", referencedColumnName = "id")
  @Getter
  @Setter
  @Nullable
  private Link link;

  @OneToOne
  @JoinColumn(name = "creator_id")
  @JsonIgnore
  @Getter
  @Setter
  private User creator;

  public static <T extends Thingy> T createThing(
      User user, T thingy, Timestamp currentUTCTimestamp) {
    final Thing thing = new Thing();
    if (thingy instanceof Note note) thing.setNote(note);
    if (thingy instanceof Link link) thing.setLink(link);
    thing.setCreator(user);
    thing.setCreatedAt(currentUTCTimestamp);
    thingy.setThing(thing);
    return thingy;
  }

  @JsonIgnore
  public boolean isDescriptionBlankHtml() {
    return getNote().isDetailsBlankHtml();
  }

  @JsonIgnore
  public ClozedString getClozeSource() {
    NoteBase source = getParentNote();
    NoteBase target = getTargetNote();
    return ClozedString.htmlClozedString(source.getTopicConstructor()).hide(target.getNoteTitle());
  }

  @JsonIgnore
  public Note getParentNote() {
    if (getLink() != null) {
      return getLink().getSourceNote();
    }
    return getNote().getParent();
  }

  @JsonIgnore
  public Note getTargetNote() {
    if (getLink() != null) {
      return getLink().getTargetNote();
    }
    return getNote().getTargetNote();
  }

  @JsonIgnore
  public String getLinkTypeLabel() {
    return getLinkType().label;
  }

  @JsonIgnore
  public Link.LinkType getLinkType() {
    if (getLink() != null) {
      return getLink().getLinkType();
    }
    return getNote().getLinkType();
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
            List.of(
                Link.LinkType.PART,
                Link.LinkType.INSTANCE,
                Link.LinkType.SPECIALIZE,
                Link.LinkType.APPLICATION));
  }
}
