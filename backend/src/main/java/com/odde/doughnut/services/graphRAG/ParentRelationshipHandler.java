package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class ParentRelationshipHandler extends RelationshipHandler {
  private boolean exhausted = false;

  public ParentRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.Parent, relatingNote);
  }

  @Override
  public Note handle() {
    if (exhausted) {
      return null;
    }
    exhausted = true;
    return relatingNote.getParent();
  }
}
