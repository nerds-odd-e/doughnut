package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "notebook_assistant")
@Data
@EqualsAndHashCode(callSuper = false)
public class NotebookAssistant extends EntityIdentifiedByIdOnly {
  @OneToOne
  @NotNull
  @JoinColumn(name = "creator_id")
  private User creator;

  @OneToOne
  @NotNull
  @JoinColumn(name = "notebook_id")
  private Notebook notebook;

  @Column(name = "assistant")
  @NotNull
  private String assistantId;

  @Column(name = "created_at")
  @NotNull
  private Timestamp createdAt;
}
