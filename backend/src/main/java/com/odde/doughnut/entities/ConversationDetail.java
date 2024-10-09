package com.odde.doughnut.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import java.util.Date;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "conversation_detail")
@Builder
public class ConversationDetail extends EntityIdentifiedByIdOnly {

  @ManyToOne
  @JoinColumn(name = "conversation_id", referencedColumnName = "id")
  Conversation conversation;

  @Column(name = "user_type")
  @Min(0)
  @Max(2)
  int userType;

  @NotNull
  @Column(name = "message")
  @Size(min = 1, max = Note.MAX_TITLE_LENGTH)
  String message;

  @Column(name = "created_at")
  @NotNull
  @Builder.Default
  @OrderBy("createdAt ASC")
  private Timestamp createdAt = new Timestamp(new Date().getTime());
}
