package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.algorithms.ClozedString;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
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
  public NoteBase getParentNote() {
    if (getLink() != null) {
      return getLink().getSourceNote();
    }
    return getNote().getParentNote();
  }

  @JsonIgnore
  public NoteBase getTargetNote() {
    if (getLink() != null) {
      return getLink().getTargetNote();
    }
    return getNote().getTargetNote();
  }
}
