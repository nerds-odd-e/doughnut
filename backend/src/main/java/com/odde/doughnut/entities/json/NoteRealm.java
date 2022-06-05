package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Note;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class NoteRealm {
  @Getter @Setter private Integer id;

  @Getter @Setter private LinksOfANote links;

  @Getter @Setter private List<Note> children;

  @Getter @Setter private Note note;
}
