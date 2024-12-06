package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class ObjectParentSiblingRelationshipHandler extends SpiralSiblingRelationshipHandler {
  private final PriorityLayer priorityFourLayer;

  public ObjectParentSiblingRelationshipHandler(
      Note relatingNote, PriorityLayer priorityFourLayer) {
    super(
        RelationshipToFocusNote.ObjectParentSibling,
        relatingNote,
        relatingNote.getTargetNote() != null ? relatingNote.getTargetNote().getParent() : null);
    this.priorityFourLayer = priorityFourLayer;
  }

  @Override
  public Note handle() {
    Note objectParentSibling = super.handle();
    if (objectParentSibling != null && priorityFourLayer != null) {
      priorityFourLayer.addHandler(
          new ObjectParentSiblingChildRelationshipHandler(objectParentSibling));
    }
    return objectParentSibling;
  }
}
