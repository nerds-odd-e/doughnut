package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;

@Entity
@Table(name = "note")
@JsonPropertyOrder({"topic", "topicConstructor", "details", "parentId", "updatedAt"})
public class LinkedNote extends NoteBase {
  public LinkedNote() {}

  @Override
  @JsonProperty
  public Note getTargetNote() {
    return super.getTargetNote();
  }
}
