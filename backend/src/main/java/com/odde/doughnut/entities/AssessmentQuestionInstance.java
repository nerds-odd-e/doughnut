package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "assessment_question_instance")
@Data
@EqualsAndHashCode(callSuper = false)
@JsonPropertyOrder({"id", "bareQuestion", "notebook", "answer"})
public class AssessmentQuestionInstance extends EntityIdentifiedByIdOnly {
  @ManyToOne
  @JoinColumn(name = "assessment_attempt_id")
  @NotNull
  @JsonIgnore
  private AssessmentAttempt assessmentAttempt;

  @OneToOne
  @JoinColumn(name = "review_question_instance_id")
  @NotNull
  @JsonIgnore // the reviewQuestionInstance should be hidden from the API
  private ReviewQuestionInstance reviewQuestionInstance;

  @NotNull
  public BareQuestion getBareQuestion() {
    return reviewQuestionInstance.getBareQuestion();
  }

  public Answer getAnswer() {
    return reviewQuestionInstance.getAnswer();
  }
}
