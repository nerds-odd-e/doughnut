package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;

public class ObjectRelationshipHandler extends RelationshipHandler {
  private boolean exhausted = false;

  public ObjectRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.Object, relatingNote);
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
