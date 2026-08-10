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

  @Column(name = "recorded_at")
  private Timestamp recordedAt;
}
