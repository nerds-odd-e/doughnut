package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "quiz_question")
@Data
@EqualsAndHashCode(callSuper = false)
@JsonPropertyOrder({"id", "checkSpell", "imageWithMask", "multipleChoicesQuestion"})
public class QuizQuestion extends EntityIdentifiedByIdOnly {
  @OneToOne
  @JoinColumn(name = "predefined_question_id", referencedColumnName = "id")
  @NotNull
  @JsonIgnore
  private PredefinedQuestion predefinedQuestion;

  public Boolean getCheckSpell() {
    return predefinedQuestion.getCheckSpell();
  }

  public ImageWithMask getImageWithMask() {
    return predefinedQuestion.getImageWithMask();
  }

  @NotNull
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    return predefinedQuestion.getMultipleChoicesQuestion();
  }

  public Notebook getNotebook() {
    return getPredefinedQuestion().getNote().getNotebook();
  }
}
