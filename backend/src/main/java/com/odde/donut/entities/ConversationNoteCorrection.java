package com.odde.donut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "conversation_note_correction")
public class ConversationNoteCorrection extends EntityIdentifiedByIdOnly {
  public enum Field {
    CONTENT,
    TITLE
  }

  public enum Decision {
    PENDING,
    DECLINED,
    ACCEPTED
  }

  @ManyToOne
  @JoinColumn(name = "source_message_id", nullable = false)
  @JsonIgnore
  private ConversationMessage sourceMessage;

  public Integer getSourceMessageId() {
    return sourceMessage.getId();
  }

  @ManyToOne
  @JoinColumn(name = "note_id", nullable = false)
  private Note note;

  @Column(name = "response_id", nullable = false)
  private String responseId;

  @Column(name = "tool_call_id", nullable = false)
  private String toolCallId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Field field;

  @Column(name = "proposed_value", columnDefinition = "MEDIUMTEXT", nullable = false)
  private String proposedValue;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String reason;

  @Column(name = "base_revision")
  private String baseRevision;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Decision decision = Decision.PENDING;

  @Column(name = "reviewed_value", columnDefinition = "MEDIUMTEXT")
  private String reviewedValue;

  @ManyToOne
  @JoinColumn(name = "decided_by")
  private User decidedBy;

  @Column(name = "decided_at")
  private Timestamp decidedAt;

  @Column(name = "accepted_revision")
  private String acceptedRevision;
}
