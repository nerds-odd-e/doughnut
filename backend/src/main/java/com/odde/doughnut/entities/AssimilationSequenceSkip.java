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
@Table(name = "assimilation_sequence_skip")
@Getter
@Setter
public class AssimilationSequenceSkip extends EntityIdentifiedByIdOnly {

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(optional = false)
  @JoinColumn(name = "note_id", nullable = false)
  private Note note;

  @Column(name = "property_key", nullable = false)
  private String propertyKey = "";

  @Column(name = "skipped_at", nullable = false)
  private Timestamp skippedAt;
}
