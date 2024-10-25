package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Table(name = "conversation")
public class Conversation extends EntityIdentifiedByIdOnly {
  @OneToMany(mappedBy = "conversation", fetch = FetchType.LAZY)
  @JsonIgnore
  List<ConversationMessage> conversationMessages = new ArrayList<>();

  @ManyToOne
  @JoinColumn(name = "assessment_question_instance_id", referencedColumnName = "id")
  AssessmentQuestionInstance assessmentQuestionInstance;

  @ManyToOne
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  Note note;

  @ManyToOne
  @JoinColumn(name = "subject_ownership_id", referencedColumnName = "id")
  Ownership subjectOwnership;

  @ManyToOne
  @Setter
  @JoinColumn(name = "conversation_initiator_id", referencedColumnName = "id")
  User conversationInitiator;

  @Column(name = "created_at")
  @NotNull
  @Setter
  private Timestamp createdAt = new Timestamp(new Date().getTime());

  @Column(name = "updated_at")
  @NotNull
  @OrderBy("updatedAt DESC")
  private Timestamp updatedAt = new Timestamp(new Date().getTime());

  public void setAssessmentQuestionInstance(AssessmentQuestionInstance assessmentQuestionInstance) {
    this.assessmentQuestionInstance = assessmentQuestionInstance;
    this.subjectOwnership =
        assessmentQuestionInstance.getAssessmentAttempt().getNotebook().getOwnership();
  }

  public void setNote(Note note) {
    this.note = note;
    this.subjectOwnership = note.getNotebook().getOwnership();
  }
}
