package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notebook_ai_assistant")
public class NotebookAiAssistant extends EntityIdentifiedByIdOnly {
  @OneToOne
  @JoinColumn(name = "notebook_id")
  @Getter
  @Setter
  private Notebook notebook;

  @Column(name = "additional_instructions_to_ai")
  @Getter
  @Setter
  private String additionalInstructionsToAi;

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt;

  @Column(name = "updated_at")
  @Getter
  @Setter
  private Timestamp updatedAt;
}
