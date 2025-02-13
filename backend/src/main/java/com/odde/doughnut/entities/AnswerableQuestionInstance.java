package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.exceptions.QuestionAnswerException;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class AnswerableQuestionInstance extends EntityIdentifiedByIdOnly {
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
  public MultipleChoicesQuestion getMultipleChoicesQuestion() {
    return predefinedQuestion.getMultipleChoicesQuestion();
  }

  public final Answer buildAnswer(AnswerDTO answerDTO) {
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
