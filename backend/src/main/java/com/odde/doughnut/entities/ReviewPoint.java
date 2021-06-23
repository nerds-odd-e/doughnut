package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
import com.odde.doughnut.models.TimestampOperations;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import javax.persistence.*;
import javax.validation.constraints.AssertTrue;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "review_point")
public class ReviewPoint {
  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Override
  public String toString() {
    return "ReviewPoint{"
        + "id=" + id + '}';
  }

  @ManyToOne
  @JoinColumn(name = "note_id")
  @Getter
  @JsonIgnore
  private Note note;

  @ManyToOne
  @JoinColumn(name = "link_id")
  @Getter
  @JsonIgnore
  private Link link;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private User user;

  @Column(name = "last_reviewed_at")
  @Getter
  @Setter
  private Timestamp lastReviewedAt = new Timestamp(System.currentTimeMillis());

  @Column(name = "next_review_at")
  @Getter
  @Setter
  private Timestamp nextReviewAt = new Timestamp(System.currentTimeMillis());

  @Column(name = "initial_reviewed_at")
  @Getter
  @Setter
  private Timestamp initialReviewedAt = new Timestamp(System.currentTimeMillis());

  @Column(name = "repetition_count")
  @Getter
  @Setter
  private Integer repetitionCount = 0;

  @Column(name = "forgetting_curve_index")
  @Getter
  @Setter
  private Integer forgettingCurveIndex =
      SpacedRepetitionAlgorithm.DEFAULT_FORGETTING_CURVE_INDEX;

  @Column(name = "removed_from_review")
  @Getter
  @Setter
  private Boolean removedFromReview = false;

  @Column(name = "note_id", insertable = false, updatable = false)
  @Getter
  private Integer noteId;

  @Column(name = "link_id", insertable = false, updatable = false)
  @Getter
  private Integer linkId;

  @Column(name = "user_id", insertable = false, updatable = false)
  @Getter
  @JsonIgnore
  private Integer userId;

  @JsonIgnore
  @Transient @Getter @Setter private Boolean repeatAgainToday = false;

  public void setNote(Note note) {
    this.note = note;
    this.noteId = null;
    if (note != null) this.noteId = note.getId();
  }

  public void setLink(Link link) {
    this.link = link;
    this.linkId = null;
    if (link != null) this.linkId = link.getId();
  }

  public boolean isInitialReviewOnSameDay(Timestamp currentTime,
                                          ZoneId timeZone) {
    return TimestampOperations.getDayId(getInitialReviewedAt(), timeZone) ==
        TimestampOperations.getDayId(currentTime, timeZone);
  }

  @JsonIgnore
  public Note getSourceNote() {
    if (link != null)
      return link.getSourceNote();
    return note;
  }

  @AssertTrue(message = "link and note cannot be both empty")
  private boolean isNotBothLinkAndNoteEmpty() {
    return note != null || link != null;
  }

  @AssertTrue(message = "cannot have both link and note")
  private boolean isNotBothLinkAndNoteNotEmpty() {
    return note == null || link == null;
  }

  public void updateMemoryState(Timestamp currentUTCTimestamp, SpacedRepetitionAlgorithm.MemoryStateChange memoryStateChange) {
      setForgettingCurveIndex(memoryStateChange.getNextForgettingCurveIndex());
      setNextReviewAt(TimestampOperations.addDaysToTimestamp(currentUTCTimestamp, memoryStateChange.getNextRepeatInDays()));
      setLastReviewedAt(currentUTCTimestamp);
  }
}
