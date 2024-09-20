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
public class AssessmentQuestionInstance extends AnswerableQuestionInstance {
  @ManyToOne
  @JoinColumn(name = "assessment_attempt_id")
  @NotNull
  @JsonIgnore
  private AssessmentAttempt assessmentAttempt;

  @NotNull
  public BareQuestion getBareQuestion() {
    return getPredefinedQuestion().getBareQuestion();
  }

  public Answer getAnswer() {
    return super.getAnswer();
  }
}
