package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "assessment_question_instance")
@Data
@EqualsAndHashCode(callSuper = false)
@JsonPropertyOrder({"id", "multipleChoicesQuestion", "notebook", "answer"})
public class AssessmentQuestionInstance extends AnswerableQuestionInstance {
  @ManyToOne
  @JoinColumn(name = "assessment_attempt_id")
  @NotNull
  @JsonIgnore
  private AssessmentAttempt assessmentAttempt;

  @Override
  @JsonProperty
  public Answer getAnswer() {
    return super.getAnswer();
  }
}
