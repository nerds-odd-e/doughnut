package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.services.TimestampService;
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
  private Integer forgettingCurveIndex = ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX;

  @Column(name = "removed_from_tracking")
  @Getter
  @Setter
  private Boolean removedFromTracking = false;

  @Column(name = "spelling")
  @Getter
  @Setter
  private Boolean spelling = false;

  private MemoryTracker() {}

  public Timestamp calculateNextRecallAt(TimestampService timestampService) {
    return timestampService.addHoursToTimestamp(
        getLastRecalledAt(), forgettingCurve().getRepeatInHours());
  }

  private ForgettingCurve forgettingCurve() {
    return new ForgettingCurve(getUser().getSpacedRepetitionAlgorithm(), getForgettingCurveIndex());
  }

  public void reviewFailed(Timestamp currentUTCTimestamp, TimestampService timestampService) {
    setForgettingCurveIndex(forgettingCurve().failed());
    setNextRecallAt(timestampService.addHoursToTimestamp(currentUTCTimestamp, 12));
  }

  public void reviewedSuccessfully(
      Timestamp currentUTCTimestamp, TimestampService timestampService) {
    long delayInHours =
        timestampService.getDiffInHours(
            currentUTCTimestamp, calculateNextRecallAt(timestampService));

    setForgettingCurveIndex(forgettingCurve().succeeded(delayInHours));

    setLastRecalledAt(currentUTCTimestamp);
    setNextRecallAt(calculateNextRecallAt(timestampService));
  }

  public void markAsRepeated(
      Timestamp currentUTCTimestamp, boolean successful, TimestampService timestampService) {
    setRepetitionCount(getRepetitionCount() + 1);
    if (successful) {
      reviewedSuccessfully(currentUTCTimestamp, timestampService);
    } else {
      reviewFailed(currentUTCTimestamp, timestampService);
    }
  }
}
