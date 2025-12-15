package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.PriorityLayer;

public class TargetParentSiblingRelationshipHandler extends SpiralSiblingRelationshipHandler {
  private final PriorityLayer priorityFourLayer;

  public TargetParentSiblingRelationshipHandler(
      Note relatingNote, PriorityLayer priorityFourLayer) {
    super(
        RelationshipToFocusNote.TargetParentSibling,
        relatingNote,
        relatingNote.getTargetNote() != null ? relatingNote.getTargetNote().getParent() : null);
    this.priorityFourLayer = priorityFourLayer;
  }

  @Override
  public Note handle() {
    Note targetParentSibling = super.handle();
    if (targetParentSibling != null && priorityFourLayer != null) {
      priorityFourLayer.addHandler(
          new TargetParentSiblingChildRelationshipHandler(targetParentSibling));
    }
    return targetParentSibling;
  }
}
