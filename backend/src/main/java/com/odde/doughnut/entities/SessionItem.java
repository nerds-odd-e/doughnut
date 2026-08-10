package com.odde.doughnut.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "session_item")
@Getter
@Setter
public class SessionItem extends EntityIdentifiedByIdOnly {

  @ManyToOne(optional = false)
  @JoinColumn(name = "learning_session_id", nullable = false)
  private LearningSession learningSession;

  @ManyToOne(optional = false)
  @JoinColumn(name = "memory_tracker_id", nullable = false)
  private MemoryTracker memoryTracker;

  @Column(name = "note_title", nullable = false)
  private String noteTitle;

  @Column(name = "feedback_score", nullable = false)
  private Integer feedbackScore;

  @Column(name = "feedback_recorded_at", nullable = false)
  private Timestamp feedbackRecordedAt;
}
