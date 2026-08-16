package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.controllers.dto.RecalledNote;
import com.odde.doughnut.utils.TimestampOperations;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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

  public static MemoryTracker buildMemoryTrackerForProperty(Note note, String propertyKey) {
    MemoryTracker entity = buildMemoryTrackerForNote(note);
    entity.setPropertyKey(propertyKey);
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

  @Column(name = "recall_count")
  @Getter
  @Setter
  private Integer recallCount = 0;

  @Column(name = "stability")
  @Getter
  @Setter
  private Float stability = ForgettingCurve.ASSIMILATE_STABILITY_HOURS;

  @Column(name = "difficulty")
  @Getter
  @Setter
  private Float difficulty;

  @Column(name = "removed_from_tracking")
  @Getter
  @Setter
  private Boolean removedFromTracking = false;

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  @Getter
  private MemoryTrackerType type = MemoryTrackerType.UNDERSTANDING;

  @Column(name = "property_key")
  @Getter
  @Setter
  private String propertyKey = "";

  @JsonProperty("spelling")
  public Boolean getSpelling() {
    return isSpelling();
  }

  public void setSpelling(Boolean spelling) {
    setType(
        Boolean.TRUE.equals(spelling)
            ? MemoryTrackerType.SPELLING
            : MemoryTrackerType.UNDERSTANDING);
  }

  public void setType(MemoryTrackerType type) {
    this.type = type == null ? MemoryTrackerType.UNDERSTANDING : type;
  }

  @JsonIgnore
  public boolean isSpelling() {
    return getType() == MemoryTrackerType.SPELLING;
  }

  @JsonIgnore
  public boolean isCommissioned() {
    return getType() == MemoryTrackerType.COMMISSIONED;
  }

  @JsonIgnore
  public boolean isUnderstanding() {
    return getType() == MemoryTrackerType.UNDERSTANDING;
  }

  /**
   * JPQL fragment for joined alias {@code rp}; must stay aligned with {@link
   * #isNoteLevelTracker()}.
   */
  public static final String JPA_WHERE_NOTE_LEVEL_TRACKER =
      "(rp.propertyKey IS NULL OR rp.propertyKey = '')";

  /**
   * JPQL fragment for joined alias {@code tmtBlock}; must stay aligned with {@link
   * #isNoteLevelTracker()}.
   */
  public static final String JPA_WHERE_NOTE_LEVEL_TARGET_TRACKER =
      "(tmtBlock.propertyKey IS NULL OR tmtBlock.propertyKey = '')";

  /** JPQL fragment for joined alias {@code rp}: ordinary assimilation is UNDERSTANDING only. */
  public static final String JPA_WHERE_UNDERSTANDING_TRACKER =
      "rp.type = com.odde.doughnut.entities.MemoryTrackerType.UNDERSTANDING";

  /**
   * JPQL fragment for joined alias {@code tmtBlock}: ordinary assimilation is UNDERSTANDING only.
   */
  public static final String JPA_WHERE_UNDERSTANDING_TARGET_TRACKER =
      "tmtBlock.type = com.odde.doughnut.entities.MemoryTrackerType.UNDERSTANDING";

  @Column(name = "deleted_at")
  @JsonIgnore
  @Getter
  @Setter
  private Timestamp deletedAt;

  @JsonProperty("latestTutorFeedbackScore")
  @Transient
  @Getter
  @Setter
  private Integer latestTutorFeedbackScore;

  private MemoryTracker() {}

  public Timestamp calculateNextRecallAt() {
    return TimestampOperations.addHoursToTimestamp(getLastRecalledAt(), Math.round(getStability()));
  }

  ForgettingCurve forgettingCurve() {
    return new ForgettingCurve(getStability(), getDifficulty());
  }

  long elapsedHoursUntil(Timestamp currentUTCTimestamp) {
    return TimestampOperations.getDiffInHours(currentUTCTimestamp, getLastRecalledAt());
  }

  void scheduleNextRecallFromStability(Timestamp currentUTCTimestamp) {
    setLastRecalledAt(currentUTCTimestamp);
    setNextRecallAt(calculateNextRecallAt());
  }

  public void recallFailed(Timestamp currentUTCTimestamp) {
    recalledAgain(currentUTCTimestamp);
    setNextRecallAt(TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, 12));
  }

  public void recalledSuccessfully(Timestamp currentUTCTimestamp, Integer thinkingTimeMs) {
    ForgettingCurve curve = forgettingCurve();
    setDifficulty(curve.difficultyAfterSuccessfulRecall());
    setStability(curve.succeeded(elapsedHoursUntil(currentUTCTimestamp), thinkingTimeMs));
    scheduleNextRecallFromStability(currentUTCTimestamp);
  }

  public void recalledEasily(Timestamp currentUTCTimestamp) {
    ForgettingCurve curve = forgettingCurve();
    setDifficulty(curve.difficultyAfterEasyRecall());
    setStability(curve.stabilityAfterEasyRecall(elapsedHoursUntil(currentUTCTimestamp)));
    scheduleNextRecallFromStability(currentUTCTimestamp);
  }

  public void recalledHard(Timestamp currentUTCTimestamp) {
    ForgettingCurve curve = forgettingCurve();
    setDifficulty(curve.difficultyAfterHardRecall());
    setStability(curve.stabilityAfterHardRecall(elapsedHoursUntil(currentUTCTimestamp)));
    scheduleNextRecallFromStability(currentUTCTimestamp);
  }

  public void recalledAgain(Timestamp currentUTCTimestamp) {
    MemoryTrackerAgainRecall.apply(this, currentUTCTimestamp);
  }

  public void markAsRecalled(
      Timestamp currentUTCTimestamp, boolean successful, Integer thinkingTimeMs) {
    setRecallCount(getRecallCount() + 1);
    if (successful) {
      recalledSuccessfully(currentUTCTimestamp, thinkingTimeMs);
    } else {
      recallFailed(currentUTCTimestamp);
    }
  }

  public void adjustForConfusion() {
    Timestamp existingDue = getNextRecallAt();
    setStability(forgettingCurve().confusionAdjusted());
    Timestamp projected = calculateNextRecallAt();
    setNextRecallAt(projected.after(existingDue) ? existingDue : projected);
  }

  @JsonIgnore
  public boolean isActive() {
    return deletedAt == null && !Boolean.TRUE.equals(removedFromTracking);
  }

  @JsonIgnore
  public boolean isNoteLevelTracker() {
    String key = getPropertyKey();
    return key == null || key.isEmpty();
  }

  @JsonProperty
  public RecalledNote getRecalledNote() {
    return RecalledNote.from(getNote(), getPropertyKey());
  }
}
