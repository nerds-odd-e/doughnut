package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "wiki_reference_migration_progress")
@Getter
@Setter
public class WikiReferenceMigrationProgress extends EntityIdentifiedByIdOnly {

  @Column(name = "step_name", nullable = false, length = 128, unique = true)
  @NotNull
  @Size(max = 128)
  private String stepName;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  @NotNull
  private WikiReferenceMigrationStepStatus status;

  @Column(name = "total_count", nullable = false)
  private int totalCount;

  @Column(name = "processed_count", nullable = false)
  private int processedCount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "last_processed_note_id", referencedColumnName = "id")
  private Note lastProcessedNote;

  @Column(name = "last_error", columnDefinition = "TEXT")
  private String lastError;

  @Column(name = "created_at", nullable = false, updatable = false)
  @NotNull
  private Timestamp createdAt;

  @Column(name = "updated_at", nullable = false)
  @NotNull
  private Timestamp updatedAt;

  @Column(name = "completed_at")
  private Timestamp completedAt;

  @PrePersist
  void onCreate() {
    Timestamp now = new Timestamp(System.currentTimeMillis());
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = new Timestamp(System.currentTimeMillis());
  }
}
