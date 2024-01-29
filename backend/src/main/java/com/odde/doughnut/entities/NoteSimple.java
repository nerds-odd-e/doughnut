package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "note")
@JsonPropertyOrder({"topic", "topicConstructor", "details", "parentId", "updatedAt"})
public class NoteSimple extends NoteBase {
  public static final int MAX_TITLE_LENGTH = 150;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Note parent;

  public NoteSimple() {}
}
