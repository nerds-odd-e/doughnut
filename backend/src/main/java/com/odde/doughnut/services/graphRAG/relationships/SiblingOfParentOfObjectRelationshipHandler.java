package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.PriorityLayer;

public class SiblingOfParentOfObjectRelationshipHandler extends SpiralSiblingRelationshipHandler {
  private final PriorityLayer priorityFourLayer;

  public SiblingOfParentOfObjectRelationshipHandler(
      Note relatingNote, PriorityLayer priorityFourLayer) {
    super(
        RelationshipToFocusNote.SiblingOfParentOfObject,
        relatingNote,
        relatingNote.getTargetNote() != null ? relatingNote.getTargetNote().getParent() : null);
    this.priorityFourLayer = priorityFourLayer;
  }

  @Override
  public Note handle() {
    Note objectParentSibling = super.handle();
    if (objectParentSibling != null && priorityFourLayer != null) {
      priorityFourLayer.addHandler(
          new ChildOfSiblingOfParentOfObjectRelationshipHandler(objectParentSibling));
    }
    return objectParentSibling;
  }
}
