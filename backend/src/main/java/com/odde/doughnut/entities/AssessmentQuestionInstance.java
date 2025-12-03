package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "assessment_question_instance")
@Data
@EqualsAndHashCode(callSuper = false)
@JsonPropertyOrder({"id", "multipleChoicesQuestion", "notebook", "answer"})
public class AssessmentQuestionInstance extends EntityIdentifiedByIdOnly {
  @ManyToOne
  @JoinColumn(name = "assessment_attempt_id")
  @NotNull
  @JsonIgnore
  private AssessmentAttempt assessmentAttempt;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "answerable_mcq_id", referencedColumnName = "id")
  @NotNull
  @JsonIgnore
  private AnswerableMCQ answerableMCQ;

  @JsonProperty
  public Answer getAnswer() {
    if (answerableMCQ == null) {
      return null;
    }
    return answerableMCQ.getAnswer();
  }

  @JsonProperty
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    if (answerableMCQ == null) {
      return null;
    }
    return answerableMCQ.getMultipleChoicesQuestion();
  }

  @JsonIgnore
  public PredefinedQuestion getPredefinedQuestion() {
    if (answerableMCQ == null) {
      return null;
    }
    return answerableMCQ.getPredefinedQuestion();
  }
}
