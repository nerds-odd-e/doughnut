package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.Date;
import javax.validation.constraints.NotNull;
import lombok.*;

@Setter
@Getter
@Entity
@Table(name = "conversation_message")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage extends EntityIdentifiedByIdOnly {

  @ManyToOne
  @JoinColumn(name = "conversation_id", referencedColumnName = "id")
  Conversation conversation;

  @NotNull
  @Column(name = "message", columnDefinition = "TEXT")
  String message;

  @ManyToOne
  @Setter
  @JoinColumn(name = "sender", referencedColumnName = "id")
  User sender;

  @Column(name = "created_at")
  @NotNull
  @Builder.Default
  @OrderBy("createdAt ASC")
  private Timestamp createdAt = new Timestamp(new Date().getTime());
}