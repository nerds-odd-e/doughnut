package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "review_question_instance")
@Data
@EqualsAndHashCode(callSuper = false)
@JsonPropertyOrder({"id", "bareQuestion", "notebook"})
public class AssessmentQuestionInstance extends EntityIdentifiedByIdOnly {
  @ManyToOne
  @JoinColumn(name = "assessment_attempt_id")
  @NotNull
  @JsonIgnore
  private AssessmentAttempt assessmentAttempt;

  @OneToOne
  @JoinColumn(name = "review_question_instance_id")
  @NotNull
  @JsonIgnore
  private ReviewQuestionInstance reviewQuestionInstance;

  @NotNull
  public BareQuestion getBareQuestion() {
    return reviewQuestionInstance.getBareQuestion();
  }
}
