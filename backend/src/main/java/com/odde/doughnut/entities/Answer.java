package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_answer")
public class Answer extends EntityIdentifiedByIdOnly {
  @Getter
  @Setter
  @Column(name = "answer")
  String spellingAnswer;

  @Getter
  @Setter
  @Column(name = "choice_index")
  Integer choiceIndex;

  @ManyToOne
  @JoinColumn(name = "quiz_question_id", referencedColumnName = "id")
  @Getter
  @Setter
  @JsonIgnore
  QuizQuestionEntity question;

  @Column(name = "created_at")
  @Getter
  @Setter
  @JsonIgnore
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @JsonIgnore
  public boolean isCorrect() {
    return question.checkAnswer(this);
  }

  @JsonIgnore
  public String getAnswerDisplay() {
    if (question != null && choiceIndex != null) {
      return question.getOptions().get(choiceIndex).getDisplay();
    }
    return getSpellingAnswer();
  }

  public void setFromDTO(AnswerDTO answerDTO) {
    setSpellingAnswer(answerDTO.getSpellingAnswer());
    setChoiceIndex(answerDTO.getChoiceIndex());
  }
}
