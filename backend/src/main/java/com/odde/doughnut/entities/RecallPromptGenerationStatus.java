package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recall_prompt_generation_status")
public class RecallPromptGenerationStatus {
  @Id
  @Column(name = "memory_tracker_id")
  @Getter
  @Setter
  private Integer memoryTrackerId;

  @ManyToOne
  @JoinColumn(name = "memory_tracker_id", insertable = false, updatable = false)
  @Getter
  @Setter
  private MemoryTracker memoryTracker;

  @Column(name = "last_attempt_time")
  @Getter
  @Setter
  private Timestamp lastAttemptTime;

  @Column(name = "attempt_count")
  @Getter
  @Setter
  @NotNull
  private Integer attemptCount = 0;

  @Column(name = "successful")
  @Getter
  @Setter
  @NotNull
  private Boolean successful = false;

  @Column(name = "error_message")
  @Getter
  @Setter
  private String errorMessage;

  @Column(name = "in_progress")
  @Getter
  @Setter
  @NotNull
  private Boolean inProgress = false;

  public RecallPromptGenerationStatus() {}

  public RecallPromptGenerationStatus(Integer memoryTrackerId) {
    this.memoryTrackerId = memoryTrackerId;
  }
}
