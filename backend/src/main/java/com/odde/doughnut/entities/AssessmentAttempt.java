package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.controllers.dto.AssessmentResult;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
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
  private Timestamp submittedAt;

  @Column(name = "answers_total")
  private int answersTotal;

  @Column(name = "answers_correct")
  private int answersCorrect;

  @OneToMany(mappedBy = "assessmentAttempt", cascade = CascadeType.ALL)
  private List<AssessmentQuestionInstance> assessmentQuestionInstances = new ArrayList<>();

  @NotNull
  public Integer getNotebookId() {
    return getNotebook().getId();
  }

  public String getNotebookTitle() {
    return getNotebook().getHeadNote().getTopicConstructor();
  }

  public Boolean getIsPass() {
    return ((double) getAnswersCorrect() / getAnswersTotal()) >= 0.8;
  }

  @JsonIgnore
  public AssessmentResult getAssessmentResult() {
    AssessmentResult assessmentResult = new AssessmentResult();
    assessmentResult.attempt = this;
    assessmentResult.setTotalCount(assessmentResult.getAttempt().getAnswersTotal());
    assessmentResult.setCorrectCount(getAnswersCorrect());
    assessmentResult.isCertified = getNotebook().isCertifiable();
    assessmentResult.notebookId = getNotebook().getId();
    return assessmentResult;
  }
}
