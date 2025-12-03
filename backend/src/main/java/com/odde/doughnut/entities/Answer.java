package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import com.odde.doughnut.exceptions.QuestionAnswerException;
import com.odde.doughnut.services.ai.MultipleChoicesQuestion;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "quiz_answer")
public class Answer extends EntityIdentifiedByIdOnly {
  @Column(name = "choice_index")
  Integer choiceIndex;

  @Column(name = "created_at")
  @Setter
  @JsonIgnore
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @Column(name = "correct")
  @Setter
  @NotNull
  private Boolean correct;

  @Column(name = "thinking_time_ms")
  @Setter
  private Integer thinkingTimeMs;

  @Column(name = "spelling_answer")
  @Setter
  private String spellingAnswer;

  @JsonIgnore
  String getAnswerDisplay(@NotNull MultipleChoicesQuestion bareQuestion) {
    if (getChoiceIndex() != null) {
      return bareQuestion.getF1__choices().get(getChoiceIndex());
    }
    return "";
  }

  public static Answer buildAnswer(
      AnswerDTO answerDTO, PredefinedQuestion predefinedQuestion, Answer existingAnswer) {
    if (existingAnswer != null) {
      throw new QuestionAnswerException("The question is already answered");
    }
    Answer answer = new Answer();
    answer.choiceIndex = answerDTO.getChoiceIndex();
    answer.setCorrect(predefinedQuestion.checkAnswer(answerDTO));
    answer.setThinkingTimeMs(answerDTO.getThinkingTimeMs());
    return answer;
  }
}
