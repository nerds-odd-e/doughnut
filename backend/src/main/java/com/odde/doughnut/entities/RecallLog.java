package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
  "tutorFeedback",
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

  /**
   * Graded outcome as {@link Grade}; {@code null} means CONFUSION (non-grade). Persisted in {@code
   * product_outcome} as the grade name or {@code CONFUSION}.
   */
  @Column(name = "product_outcome", nullable = false)
  @Convert(converter = GradeOrConfusionConverter.class)
  @JsonIgnore
  @Getter
  @Setter
  private Grade grade;

  @ManyToOne
  @JoinColumn(name = "answer_id")
  @JsonIgnore
  @Getter
  @Setter
  private Answer answer;

  @Column(name = "tutor_feedback", columnDefinition = "TEXT")
  @Getter
  @Setter
  private String tutorFeedback;

  @JsonIgnore
  public boolean isConfusion() {
    return grade == null;
  }

  public void setConfusion() {
    this.grade = null;
  }

  /** Wire name kept for API stop-safety; values are grade names or CONFUSION. */
  @JsonProperty("productOutcome")
  @Schema(
      requiredMode = Schema.RequiredMode.REQUIRED,
      allowableValues = {"AGAIN", "HARD", "GOOD", "EASY", "CONFUSION"})
  public String getProductOutcome() {
    return isConfusion() ? GradeOrConfusionConverter.CONFUSION : grade.name();
  }

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
