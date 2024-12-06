package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class ReferringCousinRelationshipHandler extends RelationshipHandler {
  private final List<Note> cousins;
  private int currentIndex = 0;

  public ReferringCousinRelationshipHandler(Note referringSubject) {
    super(RelationshipToFocusNote.ReferringCousin, referringSubject);
    this.cousins = referringSubject.getChildren();
  }

  @Override
  public Note handle() {
    if (currentIndex < cousins.size()) {
      return cousins.get(currentIndex++);
    }
    return null;
  }
}
