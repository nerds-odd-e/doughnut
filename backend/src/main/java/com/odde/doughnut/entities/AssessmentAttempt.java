package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.sql.Timestamp;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "assessment_attempt")
@Getter
@Setter
public class AssessmentAttempt extends EntityIdentifiedByIdOnly {
  @ManyToOne
  @JoinColumn(name = "user_id")
  @JsonIgnore
  private User user;

  @ManyToOne
  @JoinColumn(name = "notebook_id")
  @JsonIgnore
  private Notebook notebook;

  @Column(name = "submitted_at")
  @NotNull
  private Timestamp submittedAt;

  @Column(name = "answers_total")
  private int answersTotal;

  @Column(name = "answers_correct")
  private int answersCorrect;

  public String getNotebookTitle() {
    return getNotebook().getHeadNote().getTopicConstructor();
  }

  public Boolean getIsPass() {
    return ((double) getAnswersCorrect() / getAnswersTotal()) >= 0.8;
  }

  public AssessmentAttempt getAssessmentHistory() {
    return this;
  }
}
