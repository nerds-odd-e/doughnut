package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.controllers.dto.QuizQuestionInNotebook;
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
  @JoinColumn(name = "predefined_question_id", referencedColumnName = "id")
  @JsonIgnore
  private PredefinedQuestion predefinedQuestion;

  public Boolean getCheckSpell() {
    if (predefinedQuestion == null) {
      return null;
    }
    return predefinedQuestion.getCheckSpell();
  }

  public ImageWithMask getImageWithMask() {
    if (predefinedQuestion == null) {
      return null;
    }
    return predefinedQuestion.getImageWithMask();
  }

  @NotNull
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    if (predefinedQuestion == null) {
      return null;
    }
    return predefinedQuestion.getMultipleChoicesQuestion();
  }

  @JsonIgnore
  public QuizQuestionInNotebook getQuizQuestionInNotebook() {
    QuizQuestionInNotebook quizQuestionInNotebook = new QuizQuestionInNotebook();
    quizQuestionInNotebook.setNotebook(getPredefinedQuestion().getNote().getNotebook());
    quizQuestionInNotebook.setQuizQuestion(this);
    return quizQuestionInNotebook;
  }
}
