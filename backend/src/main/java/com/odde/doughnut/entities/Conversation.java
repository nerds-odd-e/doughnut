package com.odde.doughnut.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "conversation")
public class Conversation extends EntityIdentifiedByIdOnly {

  @ManyToOne
  @Getter
  @JoinColumn(name = "assessment_question_instance_id", referencedColumnName = "id")
  AssessmentQuestionInstance assessmentQuestionInstance;

  @ManyToOne
  @Getter
  @JoinColumn(name = "subject_ownership_id", referencedColumnName = "id")
  Ownership subjectOwnership;

  @ManyToOne
  @Getter
  @Setter
  @JoinColumn(name = "conversation_initiator_id", referencedColumnName = "id")
  User conversationInitiator;

  @Getter @Setter String message;

  public void setAssessmentQuestionInstance(AssessmentQuestionInstance assessmentQuestionInstance) {
    this.assessmentQuestionInstance = assessmentQuestionInstance;
    this.subjectOwnership =
        assessmentQuestionInstance.getAssessmentAttempt().getNotebook().getOwnership();
  }
}
