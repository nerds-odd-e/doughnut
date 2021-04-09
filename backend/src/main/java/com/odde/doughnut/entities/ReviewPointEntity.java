package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
import com.odde.doughnut.models.TimestampOperations;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.persistence.*;
import javax.validation.constraints.AssertTrue;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "review_point")
public class ReviewPointEntity {
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
  @JsonIgnore
  @Getter
  @Setter
  private Note note;

  @ManyToOne
  @JoinColumn(name = "link_id")
  @JsonIgnore
  @Getter
  @Setter
  private LinkEntity linkEntity;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private UserEntity userEntity;

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

  @Column(name = "user_id", insertable = false, updatable = false)
  @Getter
  private Integer userId;

  @Transient @Getter @Setter private Boolean repeatAgainToday = false;

  public boolean isInitialReviewOnSameDay(Timestamp currentTime,
                                          ZoneId timeZone) {
    return getDayId(getInitialReviewedAt(), timeZone) ==
        getDayId(currentTime, timeZone);
  }

  public static int getDayId(Timestamp timestamp, ZoneId timeZone) {
    ZonedDateTime systemLocalDateTime =
        TimestampOperations.getSystemLocalDateTime(timestamp);
    ZonedDateTime userLocalDateTime =
        systemLocalDateTime.withZoneSameInstant(timeZone);
    return userLocalDateTime.getYear() * 366 + userLocalDateTime.getDayOfYear();
  }

  public Note getSourceNote() {
    if (linkEntity != null)
      return linkEntity.getSourceNote();
    return note;
  }

  @AssertTrue(message = "link and note cannot be both empty")
  private boolean isNotBothLinkAndNoteEmpty() {
    return note != null || linkEntity != null;
  }

  @AssertTrue(message = "cannot have both link and note")
  private boolean isNotBothLinkAndNoteNotEmpty() {
    return note == null || linkEntity == null;
  }

  public void updateMemoryState(Timestamp currentUTCTimestamp, SpacedRepetitionAlgorithm.MemoryStateChange memoryStateChange) {
      setForgettingCurveIndex(memoryStateChange.getNextForgettingCurveIndex());
      setNextReviewAt(TimestampOperations.addDaysToTimestamp(currentUTCTimestamp, memoryStateChange.getNextRepeatInDays()));
      setLastReviewedAt(currentUTCTimestamp);
  }
}
