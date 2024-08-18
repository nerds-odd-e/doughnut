package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "assessment_attempt")
@Getter
@Setter
@JsonPropertyOrder({"id", "notebookTitle", "submittedAt", "isPass"})
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
}
