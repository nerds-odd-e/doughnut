package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;

public class TargetRelationshipHandler extends RelationshipHandler {
  private boolean exhausted = false;

  public TargetRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.Target, relatingNote);
  }

  @Override
  public Note handle() {
    if (exhausted) {
      return null;
    }
    exhausted = true;
    return relatingNote.getTargetNote();
  }
}
