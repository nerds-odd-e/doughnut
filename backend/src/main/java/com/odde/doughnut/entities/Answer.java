package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "quiz_answer")
public class Answer extends EntityIdentifiedByIdOnly {
  @Column(name = "answer")
  String spellingAnswer;

  @Column(name = "choice_index")
  Integer choiceIndex;

  @OneToOne(mappedBy = "answer")
  @JsonIgnore
  ReviewQuestionInstance reviewQuestionInstance;

  public void setReviewQuestionInstance(ReviewQuestionInstance reviewQuestionInstance) {
    this.reviewQuestionInstance = reviewQuestionInstance;
    reviewQuestionInstance.setAnswer(this);
  }

  @Column(name = "created_at")
  @Setter
  @JsonIgnore
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @Column(name = "correct")
  @Setter
  @NotNull
  private Boolean correct;

  @JsonIgnore
  public void setFromDTO(AnswerDTO answerDTO) {
    spellingAnswer = answerDTO.getSpellingAnswer();
    choiceIndex = answerDTO.getChoiceIndex();
    correct = reviewQuestionInstance.getPredefinedQuestion().checkAnswer(answerDTO);
  }
}
