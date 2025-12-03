package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

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

  @ManyToOne
  @JoinColumn(name = "predefined_question_id", referencedColumnName = "id")
  @NotNull
  @JsonIgnore
  private PredefinedQuestion predefinedQuestion;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "quiz_answer_id", referencedColumnName = "id")
  @Getter
  @Setter
  @JsonIgnore
  Answer answer;

  @JsonProperty
  public Answer getAnswer() {
    return answer;
  }

  @JsonProperty
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    return predefinedQuestion.getMultipleChoicesQuestion();
  }

  @JsonIgnore
  public PredefinedQuestion getPredefinedQuestion() {
    return predefinedQuestion;
  }

  public Answer buildAnswer(com.odde.doughnut.controllers.dto.AnswerDTO answerDTO) {
    this.answer = Answer.buildAnswer(answerDTO, predefinedQuestion, answer);
    return answer;
  }
}
