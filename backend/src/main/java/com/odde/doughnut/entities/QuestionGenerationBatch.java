package com.odde.doughnut.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "question_generation_batch")
@Getter
@Setter
public class QuestionGenerationBatch extends EntityIdentifiedByIdOnly {

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private QuestionGenerationBatchStatus status;

  @Column(name = "planned_at", nullable = false)
  private Timestamp plannedAt;

  @Column(name = "openai_input_file_id")
  private String openaiInputFileId;

  @Column(name = "openai_batch_id")
  private String openaiBatchId;

  @Column(name = "submitted_at")
  private Timestamp submittedAt;
}
