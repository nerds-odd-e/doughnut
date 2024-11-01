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

  @Embedded private ConversationSubject subject = new ConversationSubject();

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

  @Column(name = "ai_assistant_thread_id")
  @Setter
  private String aiAssistantThreadId;

  @Column(name = "last_ai_assistant_thread_sync")
  @Setter
  private Timestamp lastAiAssistantThreadSync;

  @JsonIgnore
  public void setAssessmentQuestionInstance(AssessmentQuestionInstance assessmentQuestionInstance) {
    this.subject.setAssessmentQuestionInstance(assessmentQuestionInstance);
    this.subjectOwnership =
        assessmentQuestionInstance.getAssessmentAttempt().getNotebook().getOwnership();
  }

  @JsonIgnore
  public void setNote(Note note) {
    this.subject.setNote(note);
    this.subjectOwnership = note.getNotebook().getOwnership();
  }
}
