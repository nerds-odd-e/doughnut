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
@Table(name = "learning_session")
@Getter
@Setter
public class LearningSession extends EntityIdentifiedByIdOnly {

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(optional = false)
  @JoinColumn(name = "notebook_id", nullable = false)
  private Notebook notebook;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private LearningSessionStatus status;

  @Column(name = "commissioned_at", nullable = false)
  private Timestamp commissionedAt;

  @Column(name = "recorded_at")
  private Timestamp recordedAt;
}
