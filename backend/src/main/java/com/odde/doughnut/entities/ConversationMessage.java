package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
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
  @JsonIgnore
  Conversation conversation;

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  @Column(name = "message", columnDefinition = "TEXT")
  String message;

  @ManyToOne
  @Setter
  @JoinColumn(name = "sender", referencedColumnName = "id")
  User sender;

  @Column(name = "read_by_receiver", columnDefinition = "BOOLEAN")
  @Builder.Default
  private Boolean readByReceiver = false;

  @Column(name = "created_at")
  @Builder.Default
  @OrderBy("createdAt ASC")
  private Timestamp createdAt = new Timestamp(new Date().getTime());
}
