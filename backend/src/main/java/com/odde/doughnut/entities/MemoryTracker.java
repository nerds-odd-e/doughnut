package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.utils.TimestampOperations;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "memory_tracker")
public class MemoryTracker extends EntityIdentifiedByIdOnly {
  public static MemoryTracker buildMemoryTrackerForNote(Note note) {
    MemoryTracker entity = new MemoryTracker();
    entity.setNote(note);
    return entity;
  }

  @Override
  public String toString() {
    return "MemoryTracker{" + "id=" + id + '}';
  }

  @ManyToOne
  @JoinColumn(name = "note_id")
  @Getter
  @Setter
  @NotNull
  private Note note;

  @ManyToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "user_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private User user;

  @Column(name = "last_recalled_at")
  @Getter
  @Setter
  private Timestamp lastRecalledAt;

  @Column(name = "next_recall_at")
  @Getter
  @Setter
  @NotNull
  private Timestamp nextRecallAt;

  @Column(name = "assimilated_at")
  @Getter
  @Setter
  private Timestamp assimilatedAt;

  @Column(name = "repetition_count")
  @Getter
  @Setter
  private Integer repetitionCount = 0;

  @Column(name = "forgetting_curve_index")
  @Getter
  @Setter
  private Float forgettingCurveIndex = ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX;

  @Column(name = "removed_from_tracking")
  @Getter
  @Setter
  private Boolean removedFromTracking = false;

  @Column(name = "spelling")
  @Getter
  @Setter
  private Boolean spelling = false;

  @Column(name = "deleted_at")
  @JsonIgnore
  @Getter
  @Setter
  private Timestamp deletedAt;

  private MemoryTracker() {}

  public Timestamp calculateNextRecallAt() {
    return TimestampOperations.addHoursToTimestamp(
        getLastRecalledAt(), forgettingCurve().getRepeatInHours());
  }

  private ForgettingCurve forgettingCurve() {
    return new ForgettingCurve(getUser().getSpacedRepetitionAlgorithm(), getForgettingCurveIndex());
  }

  public void reviewFailed(Timestamp currentUTCTimestamp) {
    setForgettingCurveIndex(forgettingCurve().failed());
    setNextRecallAt(TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, 12));
  }

  public void reviewedSuccessfully(Timestamp currentUTCTimestamp, Integer thinkingTimeMs) {
    long delayInHours =
        TimestampOperations.getDiffInHours(currentUTCTimestamp, calculateNextRecallAt());

    setForgettingCurveIndex(forgettingCurve().succeeded(delayInHours, thinkingTimeMs));

    setLastRecalledAt(currentUTCTimestamp);
    setNextRecallAt(calculateNextRecallAt());
  }

  public void markAsRepeated(
      Timestamp currentUTCTimestamp, boolean successful, Integer thinkingTimeMs) {
    setRepetitionCount(getRepetitionCount() + 1);
    if (successful) {
      reviewedSuccessfully(currentUTCTimestamp, thinkingTimeMs);
    } else {
      reviewFailed(currentUTCTimestamp);
    }
  }

  @JsonIgnore
  public boolean isActive() {
    return deletedAt == null
        && !Boolean.TRUE.equals(removedFromTracking)
        && note.getDeletedAt() == null;
  }
}
