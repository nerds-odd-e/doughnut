package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
@JsonPropertyOrder({
  "id",
  "totalQuestionCount",
  "answersCorrect",
  "notebookId",
  "notebookTitle",
  "submittedAt",
  "isPass",
  "isCertifiable"
})
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
  private int totalQuestionCount;

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
    return ((double) getAnswersCorrect() / getTotalQuestionCount()) >= 0.8;
  }

  public boolean isCertifiable() {
    return getNotebook().isCertifiable();
  }

  public void buildAssessmentQuestionInstance(PredefinedQuestion predefinedQuestion) {
    AssessmentQuestionInstance assessmentQuestionInstance = new AssessmentQuestionInstance();
    assessmentQuestionInstance.setAssessmentAttempt(this);
    AnswerableMCQ answerableMCQ = new AnswerableMCQ();
    answerableMCQ.setPredefinedQuestion(predefinedQuestion);
    assessmentQuestionInstance.setAnswerableMCQ(answerableMCQ);
    getAssessmentQuestionInstances().add(assessmentQuestionInstance);
  }

  public void buildQuestions(List<PredefinedQuestion> questions) {
    setTotalQuestionCount(questions.size());
    questions.forEach(this::buildAssessmentQuestionInstance);
  }
}
