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

  @JsonIgnore
  public void setRecallPrompt(RecallPrompt recallPrompt) {
    this.subject.setRecallPrompt(recallPrompt);
    this.subjectOwnership = recallPrompt.getNotebook().getOwnership();
  }

  @JsonIgnore
  public List<ConversationMessage> getUnseenMessagesByAssistant() {
    return getConversationMessages().stream()
        .filter(
            msg ->
                getLastAiAssistantThreadSync() == null
                    || msg.getCreatedAt().after(getLastAiAssistantThreadSync()))
        .filter(msg -> msg.getSender() != null)
        .toList();
  }

  @JsonIgnore
  public Note getSubjectNote() {
    Note note = subject.getNote();
    if (note != null) {
      return note;
    }

    RecallPrompt recallPrompt = subject.getRecallPrompt();
    if (recallPrompt != null) {
      return recallPrompt.getPredefinedQuestion().getNote();
    }

    AssessmentQuestionInstance assessmentQuestionInstance = subject.getAssessmentQuestionInstance();
    if (assessmentQuestionInstance != null) {
      return assessmentQuestionInstance.getPredefinedQuestion().getNote();
    }

    return null;
  }

  @JsonIgnore
  public String getAdditionalContextForSubject() {
    RecallPrompt recallPrompt = subject.getRecallPrompt();
    if (recallPrompt != null) {
      return """
          User attempted to answer the following question about the note of focus.
          Please note that user is not prompted with the specific note of focus,
          but only with the broader notebook name. A question that is not possible to answer
          is regarded as a wrong question.
          Here's the question definition and user's answer:

          """
          + recallPrompt.getQuestionDetails();
    }

    return null;
  }

  @JsonIgnore
  public String getContextDescription() {
    Note note = getSubjectNote();
    if (note == null) {
      return "Unknown conversation subject";
    }

    String context = note.getNoteDescription();
    String additionalContext = getAdditionalContextForSubject();
    if (additionalContext != null) {
      context += "\n\n" + additionalContext;
    }

    return context;
  }
}
