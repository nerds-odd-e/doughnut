package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.LinkType;
import com.odde.doughnut.entities.Note;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class NoteRealm {

  @Getter @Setter private Map<LinkType, LinkViewed> links;

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
}
