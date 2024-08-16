package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.odde.doughnut.controllers.dto.AssessmentHistory;
import jakarta.persistence.*;
import java.sql.Timestamp;
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
  private Notebook notebook;

  @Column(name = "submitted_at")
  private Timestamp submittedAt;

  @Column(name = "answers_total")
  private int answersTotal;

  @Column(name = "answers_correct")
  private int answersCorrect;

  public AssessmentHistory getAssessmentHistory() {
    return new AssessmentHistory(
        getId(),
        getNotebook().getHeadNote().getTopicConstructor(),
        getSubmittedAt(),
        ((double) getAnswersCorrect() / getAnswersTotal()) >= 0.8);
  }
}
