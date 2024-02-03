package com.odde.doughnut.controllers.json;

import com.odde.doughnut.entities.LinkType;
import com.odde.doughnut.entities.Note;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class NoteRealm {

  @Getter @Setter private Map<LinkType, LinkViewed> links;


  @Getter private Note note;

  @Getter @Setter private NotePositionViewedByUser notePosition;

  public NoteRealm(Note note) {
    this.note = note;
  }

  public Integer getId() {
    return note.getId();
  }

  public List<Note> getChildren() {
    return note.getChildren();
  }
}
