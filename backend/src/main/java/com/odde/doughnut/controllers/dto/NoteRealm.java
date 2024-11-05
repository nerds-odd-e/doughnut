package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@JsonPropertyOrder({"id", "note", "fromBazaar", "circle", "children", "refers", "notebook"})
public class NoteRealm {
  @Getter @Setter private List<Note> refers;

  @NotNull @Getter private Note note;

  @Getter @Setter private Boolean fromBazaar;

  public NoteRealm(Note note) {
    this.note = note;
  }

  @NotNull
  public Integer getId() {
    return note.getId();
  }

  public List<Note> getChildren() {
    return note.getChildren();
  }

  public Circle getCircle() {
    return note.getNotebook().getOwnership().getCircle();
  }

  @NotNull
  public Notebook getNotebook() {
    return note.getNotebook();
  }
}
