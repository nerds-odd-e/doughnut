package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.odde.doughnut.algorithms.NoteTitle;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
public abstract class Notey extends Thingy {

  @OneToOne(mappedBy = "note", cascade = CascadeType.ALL)
  @Getter
  @Setter
  @JsonIgnore
  private Thing thing;

  @ManyToOne
  @JoinColumn(name = "notebook_id", referencedColumnName = "id")
  @JsonIgnore
  @Getter
  @Setter
  private Notebook notebook;

  @Embedded @Valid @Getter private final NoteAccessories noteAccessories = new NoteAccessories();

  @Column(name = "description")
  @Getter
  @Setter
  @JsonPropertyDescription("The details of the note is in markdown format.")
  private String details;

  @Size(min = 1, max = NoteSimple.MAX_TITLE_LENGTH)
  @Getter
  @Setter
  @Column(name = "topic_constructor")
  private String topicConstructor = "";

  @Column(name = "deleted_at")
  @Getter
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Timestamp deletedAt;

  public void setDeletedAt(Timestamp value) {
    this.deletedAt = value;
    if (this.thing != null) this.thing.setDeletedAt(value);
  }

  @JsonIgnore
  public NoteTitle getNoteTitle() {
    return new NoteTitle(getTopicConstructor());
  }
}
