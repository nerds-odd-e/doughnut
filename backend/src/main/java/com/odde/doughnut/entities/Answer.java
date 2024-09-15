package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.dto.AnswerDTO;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_answer")
public class Answer extends EntityIdentifiedByIdOnly {
  @Getter
  @Column(name = "answer")
  String spellingAnswer;

  @Getter
  @Column(name = "choice_index")
  Integer choiceIndex;

  @OneToOne
  @JoinColumn(name = "review_question_instance_id", referencedColumnName = "id")
  @Getter
  @Setter
  @JsonIgnore
  ReviewQuestionInstance reviewQuestionInstance;

  @Column(name = "created_at")
  @Getter
  @Setter
  @JsonIgnore
  private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

  @Column(name = "correct")
  @Getter
  @Setter
  @NotNull
  private Boolean correct;

  @JsonIgnore
  public String getAnswerDisplay() {
    if (reviewQuestionInstance != null && choiceIndex != null) {
      return reviewQuestionInstance
          .getBareQuestion()
          .getMultipleChoicesQuestion()
          .getChoices()
          .get(choiceIndex);
    }
    return getSpellingAnswer();
  }

  public void setFromDTO(AnswerDTO answerDTO) {
    spellingAnswer = answerDTO.getSpellingAnswer();
    choiceIndex = answerDTO.getChoiceIndex();
    correct = reviewQuestionInstance.getPredefinedQuestion().checkAnswer(answerDTO);
  }
}
