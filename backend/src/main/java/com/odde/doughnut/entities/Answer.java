package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

  @JsonIgnore
  String getAnswerDisplay(@NotNull MultipleChoicesQuestion bareQuestion) {
    if (getChoiceIndex() != null) {
      return bareQuestion.getF1__choices().get(getChoiceIndex());
    }
    return "";
  }
}
