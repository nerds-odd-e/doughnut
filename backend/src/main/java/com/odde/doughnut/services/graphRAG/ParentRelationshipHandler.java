package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class ParentRelationshipHandler extends RelationshipHandler {
  private boolean exhausted = false;

  public ParentRelationshipHandler() {
    super(RelationshipToFocusNote.Parent);
  }

  @Override
  public Note handle(Note focusNote) {
    if (exhausted) {
      return null;
    }
    exhausted = true;
    return focusNote.getParent();
  }
}
