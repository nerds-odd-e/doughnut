package com.odde.donut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "daily_probe")
@Getter
@Setter
public class DailyProbe extends EntityIdentifiedByIdOnly {

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnore
  private User user;

  @Column(name = "completed_at", nullable = false)
  private Timestamp completedAt;

  @Column(name = "speed")
  private Double speed;

  @Column(name = "accuracy", nullable = false)
  private Integer accuracy;

  @Column(name = "lapse_count", nullable = false)
  private Integer lapseCount;

  @Column(name = "variability")
  private Double variability;

  @Column(name = "trials_json", nullable = false, columnDefinition = "TEXT")
  private String trialsJson;
}
