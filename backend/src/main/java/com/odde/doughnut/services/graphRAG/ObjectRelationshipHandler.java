package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class ObjectRelationshipHandler extends RelationshipHandler {
  private boolean exhausted = false;

  public ObjectRelationshipHandler() {
    super(RelationshipToFocusNote.Object);
  }

  @Override
  public Note handle(Note focusNote) {
    if (exhausted) {
      return null;
    }
    exhausted = true;
    return focusNote.getTargetNote();
  }
}
