package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.Link;
import com.odde.doughnut.entities.Note;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class NoteRealm {
  @Getter @Setter private Integer id;

  @Getter @Setter private Map<Link.LinkType, LinkViewed> links;

  @Getter @Setter private List<Note> children;

  @Getter @Setter private Note note;

  @Getter @Setter private NotePositionViewedByUser notePosition;
}
