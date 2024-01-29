package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;

@Entity
@Table(name = "note")
@JsonPropertyOrder({"topic", "topicConstructor", "details", "parentId", "updatedAt"})
public class NoteSimple extends NoteBase {
  public static final int MAX_TITLE_LENGTH = 150;

  public NoteSimple() {}
}
