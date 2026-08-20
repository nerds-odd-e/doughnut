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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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

  @OneToMany(mappedBy = "memoryTracker")
  @Fetch(FetchMode.SUBSELECT)
  @JsonIgnore
  private List<RecallLog> recallLogs = new ArrayList<>();

  @JsonProperty
  public Integer getRecallCount() {
    int count = 0;
    for (RecallLog log : recallLogs) {
      if (!log.isConfusion()) {
        count++;
      }
    }
    return count;
  }

  public void addRecallLog(RecallLog recallLog) {
    recallLogs.add(recallLog);
  }

  @Column(name = "stability")
  @Getter
  @Setter
  private Float stability = Fsrs.NEW_STABILITY_HOURS;

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

  @JsonIgnore
  public boolean isNew() {
    return Fsrs.isNew(getStability());
  }

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
    return MemoryTrackerRecallDue.calculateNextRecallAt(this);
  }

  public long elapsedHoursUntil(Timestamp currentUTCTimestamp) {
    return MemoryTrackerRecallDue.elapsedHoursUntil(this, currentUTCTimestamp);
  }

  void scheduleNextRecallFromStability(Timestamp currentUTCTimestamp) {
    setLastRecalledAt(currentUTCTimestamp);
    Timestamp scheduled = calculateNextRecallAt();
    if (!scheduled.after(currentUTCTimestamp)) {
      scheduled =
          TimestampOperations.addHoursToTimestamp(
              currentUTCTimestamp, Fsrs.intervalHours(Fsrs.STRICTLY_FUTURE_FALLBACK_HOURS));
    }
    setNextRecallAt(scheduled);
  }

  public void applyGrade(Timestamp now, Grade grade) {
    long elapsed = elapsedHoursUntil(now);
    Float stability = getStability();
    Float difficulty = getDifficulty();
    Fsrs.NextMemory next =
        switch (grade) {
          case GOOD -> Fsrs.afterGoodRecall(stability, difficulty, elapsed);
          case EASY -> Fsrs.afterEasyRecall(stability, difficulty, elapsed);
          case HARD -> Fsrs.afterHardRecall(stability, difficulty, elapsed);
          case AGAIN -> Fsrs.afterAgainRecall(stability, difficulty, elapsed);
        };
    setDifficulty(next.difficulty());
    MemoryTrackerNextStability.write(this, next.stability());
    scheduleNextRecallFromStability(now);
  }

  public void adjustForConfusion(Timestamp currentUTCTimestamp) {
    Timestamp existingDue = getNextRecallAt();
    MemoryTrackerNextStability.write(
        this,
        Fsrs.confusionAdjusted(
            getStability(), getDifficulty(), elapsedHoursUntil(currentUTCTimestamp)));
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
