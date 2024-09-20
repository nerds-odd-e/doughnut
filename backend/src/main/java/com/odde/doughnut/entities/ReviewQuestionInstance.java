package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.exceptions.QuestionAnswerException;
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

  @OneToOne(cascade = CascadeType.ALL)
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
    if (answer == null) {
      return null;
    }

    AnsweredQuestion answerResult = new AnsweredQuestion();
    answerResult.answer = answer;
    answerResult.note = getPredefinedQuestion().getNote();
    answerResult.predefinedQuestion = getPredefinedQuestion();
    answerResult.answerDisplay = answer.getAnswerDisplay(this.getBareQuestion());
    answerResult.reviewQuestionInstanceId = id;
    return answerResult;
  }

  public Answer buildAnswer(AnswerDTO answerDTO) {
    if (getAnswer() != null) {
      throw new QuestionAnswerException("The question is already answered");
    }
    this.answer = new Answer();
    this.answer.spellingAnswer = answerDTO.getSpellingAnswer();
    this.answer.choiceIndex = answerDTO.getChoiceIndex();
    this.answer.setCorrect(getPredefinedQuestion().checkAnswer(answerDTO));
    return answer;
  }
}
