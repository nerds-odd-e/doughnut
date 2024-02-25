package com.odde.doughnut.controllers.json;

import com.odde.doughnut.entities.HierarchicalNote;
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

  @NotNull @Getter @Setter private NotePositionViewedByUser notePosition;

  public NoteRealm(Note note) {
    this.note = note;
  }

  @NotNull
  public Integer getId() {
    return note.getId();
  }

  public List<HierarchicalNote> getChildren() {
    return note.getChildren();
  }
}
