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
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "question_and_answer_id", referencedColumnName = "id")
  @JsonIgnore
  private QuestionAndAnswer questionAndAnswer;

  public Boolean getCheckSpell() {
    if (questionAndAnswer == null) {
      return null;
    }
    return questionAndAnswer.getCheckSpell();
  }

  public ImageWithMask getImageWithMask() {
    if (questionAndAnswer == null) {
      return null;
    }
    return questionAndAnswer.getImageWithMask();
  }

  @NotNull
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    if (questionAndAnswer == null) {
      return null;
    }
    return questionAndAnswer.getMultipleChoicesQuestion();
  }
}
