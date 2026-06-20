package com.odde.doughnut.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "question_generation_batch_maintenance_run")
@Getter
@Setter
public class QuestionGenerationBatchMaintenanceRun extends EntityIdentifiedByIdOnly {

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_source", nullable = false)
  private QuestionGenerationBatchMaintenanceTriggerSource triggerSource;

  @Column(name = "started_at", nullable = false)
  private Timestamp startedAt;

  @Column(name = "finished_at")
  private Timestamp finishedAt;

  @Column(name = "error")
  private String error;

  @Column(name = "considered_user_count")
  private Integer consideredUserCount;

  @Column(name = "submitted_count")
  private Integer submittedCount;

  @Column(name = "failed_count")
  private Integer failedCount;

  @Column(name = "skipped_count")
  private Integer skippedCount;
}
