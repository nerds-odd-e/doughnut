package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "review_question_instance")
@Data
@EqualsAndHashCode(callSuper = false)
@JsonPropertyOrder({"id", "bareQuestion", "notebook"})
public class ReviewQuestionInstance extends EntityIdentifiedByIdOnly {
  @OneToOne
  @JoinColumn(name = "predefined_question_id", referencedColumnName = "id")
  @NotNull
  @JsonIgnore
  private PredefinedQuestion predefinedQuestion;

  @OneToOne(mappedBy = "reviewQuestionInstance")
  @Getter
  @Setter
  @JsonIgnore
  Answer answer;

  @NotNull
  public BareQuestion getBareQuestion() {
    return predefinedQuestion.getBareQuestion();
  }

  public Notebook getNotebook() {
    return predefinedQuestion.getNote().getNotebook();
  }
}
