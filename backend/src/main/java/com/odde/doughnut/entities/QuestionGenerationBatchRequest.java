package com.odde.doughnut.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "question_generation_batch_request")
@Getter
@Setter
public class QuestionGenerationBatchRequest extends EntityIdentifiedByIdOnly {

  @ManyToOne(optional = false)
  @JoinColumn(name = "batch_id", nullable = false)
  private QuestionGenerationBatch batch;

  @ManyToOne(optional = false)
  @JoinColumn(name = "memory_tracker_id", nullable = false)
  private MemoryTracker memoryTracker;

  @Column(name = "custom_id", nullable = false)
  private String customId;

  @Column(name = "context_seed", nullable = false)
  private Long contextSeed;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private QuestionGenerationBatchRequestStatus status =
      QuestionGenerationBatchRequestStatus.PENDING;

  @Column(name = "raw_success_payload", columnDefinition = "LONGTEXT")
  private String rawSuccessPayload;

  @Column(name = "raw_error_payload", columnDefinition = "LONGTEXT")
  private String rawErrorPayload;

  @Column(name = "error_detail", columnDefinition = "TEXT")
  private String errorDetail;

  public static String customIdFor(Integer batchId, Integer memoryTrackerId) {
    return "qgb-" + batchId + "-mt-" + memoryTrackerId;
  }
}
