package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recall_log")
@JsonPropertyOrder({
  "id",
  "recordedAt",
  "elapsedHours",
  "productOutcome",
  "memoryTrackerId",
  "answerId"
})
public class RecallLog extends EntityIdentifiedByIdOnly {
  @ManyToOne(optional = false)
  @JoinColumn(name = "memory_tracker_id", nullable = false)
  @JsonIgnore
  @Getter
  @Setter
  private MemoryTracker memoryTracker;

  @Column(name = "recorded_at", nullable = false)
  @Getter
  @Setter
  @NotNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Timestamp recordedAt;

  @Column(name = "elapsed_hours", nullable = false)
  @Getter
  @Setter
  @NotNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer elapsedHours;

  @Column(name = "product_outcome", nullable = false)
  @Enumerated(EnumType.STRING)
  @Getter
  @Setter
  @NotNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private ProductOutcome productOutcome;

  @ManyToOne
  @JoinColumn(name = "answer_id")
  @JsonIgnore
  @Getter
  @Setter
  private Answer answer;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public Integer getMemoryTrackerId() {
    return memoryTracker.getId();
  }

  @JsonProperty
  public Integer getAnswerId() {
    return answer == null ? null : answer.getId();
  }
}
