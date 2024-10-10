package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Date;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "conversation")
public class Conversation extends EntityIdentifiedByIdOnly {

  @ManyToOne
  @JoinColumn(name = "assessment_question_instance_id", referencedColumnName = "id")
  AssessmentQuestionInstance assessmentQuestionInstance;

  @ManyToOne
  @JoinColumn(name = "subject_ownership_id", referencedColumnName = "id")
  Ownership subjectOwnership;

  @ManyToOne
  @Setter
  @JoinColumn(name = "conversation_initiator_id", referencedColumnName = "id")
  User conversationInitiator;

  @Setter String message;

  @Column(name = "created_at")
  @NotNull
  private Timestamp createdAt = new Timestamp(new Date().getTime());

  @Column(name = "updated_at")
  @NotNull
  @OrderBy("updatedAt DESC")
  private Timestamp updatedAt = new Timestamp(new Date().getTime());

  @Setter
  @Column(name = "marker")
  private Boolean marker = false;

  public void setAssessmentQuestionInstance(AssessmentQuestionInstance assessmentQuestionInstance) {
    this.assessmentQuestionInstance = assessmentQuestionInstance;
    this.subjectOwnership =
        assessmentQuestionInstance.getAssessmentAttempt().getNotebook().getOwnership();
  }
}
