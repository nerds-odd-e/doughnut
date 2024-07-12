package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "user_assistant_thread")
@Data
@EqualsAndHashCode(callSuper = false)
public class UserAssistantThread extends EntityIdentifiedByIdOnly {
  @OneToOne
  @NotNull
  @JoinColumn(name = "user_id")
  private User user;

  @OneToOne
  @NotNull
  @JoinColumn(name = "note_id")
  private Note note;

  @Column(name = "thread_id")
  @NotNull
  private String threadId;
}
