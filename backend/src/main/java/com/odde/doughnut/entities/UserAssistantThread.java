package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Entity
@Table(name = "user_assistant_threads")
@Data
public class UserAssistantThread extends EntityIdentifiedByIdOnly {
  @OneToOne
  @NotNull
  @JoinColumn(name = "user_id")
  private User creatorEntity;

  @OneToOne
  @NotNull
  @JoinColumn(name = "note_id")
  private Note note;

  @Column(name = "assistant_id")
  @NotNull
  private String assistantId;
}
