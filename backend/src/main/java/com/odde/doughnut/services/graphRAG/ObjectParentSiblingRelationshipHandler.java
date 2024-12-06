package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class ObjectParentSiblingRelationshipHandler extends SpiralSiblingRelationshipHandler {
  public ObjectParentSiblingRelationshipHandler(Note relatingNote) {
    super(
        RelationshipToFocusNote.ObjectParentSibling,
        relatingNote,
        relatingNote.getTargetNote() != null ? relatingNote.getTargetNote().getParent() : null);
  }
}
