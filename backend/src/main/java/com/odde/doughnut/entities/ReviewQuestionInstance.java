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

  @OneToOne
  @JoinColumn(name = "quiz_answer_id", referencedColumnName = "id")
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

  @JsonIgnore
  public AnsweredQuestion getAnsweredQuestion() {
    Answer answer1 = getAnswer();
    AnsweredQuestion answerResult = new AnsweredQuestion();
    answerResult.answer = answer1;
    answerResult.note = getPredefinedQuestion().getNote();
    answerResult.predefinedQuestion = getPredefinedQuestion();
    String result;
    if (answer1.getChoiceIndex() != null) {
      result =
          getBareQuestion().getMultipleChoicesQuestion().getChoices().get(answer1.getChoiceIndex());
    } else {
      result = answer1.getSpellingAnswer();
    }
    answerResult.answerDisplay = result;
    return answerResult;
  }
}
