package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.algorithms.CommissionedLearningSessionFeedbackPolicy;
import com.odde.doughnut.algorithms.SpacedRepetitionAlgorithm;
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

  @Column(name = "forgetting_curve_index")
  @Getter
  @Setter
  private Float forgettingCurveIndex = ForgettingCurve.DEFAULT_FORGETTING_CURVE_INDEX;

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

  /** JPQL fragment for joined alias {@code rp}: ordinary assimilation ignores COMMISSIONED. */
  public static final String JPA_WHERE_NOT_COMMISSIONED_TRACKER =
      "rp.type <> com.odde.doughnut.entities.MemoryTrackerType.COMMISSIONED";

  /**
   * JPQL fragment for joined alias {@code tmtBlock}: ordinary assimilation ignores COMMISSIONED.
   */
  public static final String JPA_WHERE_NOT_COMMISSIONED_TARGET_TRACKER =
      "tmtBlock.type <> com.odde.doughnut.entities.MemoryTrackerType.COMMISSIONED";

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
    return TimestampOperations.addHoursToTimestamp(
        getLastRecalledAt(), forgettingCurve().getRepeatInHours());
  }

  private ForgettingCurve forgettingCurve() {
    return new ForgettingCurve(getUser().getSpacedRepetitionAlgorithm(), getForgettingCurveIndex());
  }

  public void recallFailed(Timestamp currentUTCTimestamp) {
    setForgettingCurveIndex(forgettingCurve().failed());
    setNextRecallAt(TimestampOperations.addHoursToTimestamp(currentUTCTimestamp, 12));
  }

  public void recalledSuccessfully(Timestamp currentUTCTimestamp, Integer thinkingTimeMs) {
    long delayInHours =
        TimestampOperations.getDiffInHours(currentUTCTimestamp, calculateNextRecallAt());

    setForgettingCurveIndex(forgettingCurve().succeeded(delayInHours, thinkingTimeMs));

    setLastRecalledAt(currentUTCTimestamp);
    setNextRecallAt(calculateNextRecallAt());
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

  public void markAsAccidentalMatch(Timestamp currentUTCTimestamp) {
    setRecallCount(getRecallCount() + 1);
    setForgettingCurveIndex(forgettingCurve().partialFail());
    setLastRecalledAt(currentUTCTimestamp);
    setNextRecallAt(calculateNextRecallAt());
  }

  public void recordCommissionedFeedback(Timestamp now, int score) {
    setRecallCount(getRecallCount() + 1);
    setLastRecalledAt(now);
    setForgettingCurveIndex(
        CommissionedLearningSessionFeedbackPolicy.applyScore(getForgettingCurveIndex(), score));
    setNextRecallAt(ensureNextRecallStrictlyAfterNow(now));
  }

  public void restorePreSessionSnapshot(SessionItem item) {
    setForgettingCurveIndex(item.getPreSessionForgettingCurveIndex());
    setRecallCount(item.getPreSessionRecallCount());
  }

  private Timestamp ensureNextRecallStrictlyAfterNow(Timestamp now) {
    Timestamp scheduled = calculateNextRecallAt();
    if (scheduled.after(now)) {
      return scheduled;
    }
    return TimestampOperations.addHoursToTimestamp(now, firstPositiveSpacingHours());
  }

  private int firstPositiveSpacingHours() {
    SpacedRepetitionAlgorithm algorithm = getUser().getSpacedRepetitionAlgorithm();
    for (int spacingIndex = 0; spacingIndex < 30; spacingIndex++) {
      int hours = algorithm.getRepeatInHours(spacingIndex);
      if (hours > 0) {
        return hours;
      }
    }
    return 24;
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
