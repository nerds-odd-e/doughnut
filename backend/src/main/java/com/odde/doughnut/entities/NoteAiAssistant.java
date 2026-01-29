package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "note_ai_assistant")
public class NoteAiAssistant extends EntityIdentifiedByIdOnly {
  @OneToOne
  @JoinColumn(name = "note_id")
  @Getter
  @Setter
  private Note note;

  @Column(name = "additional_instructions_to_ai")
  @Getter
  @Setter
  private String additionalInstructionsToAi;

  @Column(name = "apply_to_children")
  @Getter
  @Setter
  private Boolean applyToChildren = false;

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt;

  @Column(name = "updated_at")
  @Getter
  @Setter
  private Timestamp updatedAt;
}
