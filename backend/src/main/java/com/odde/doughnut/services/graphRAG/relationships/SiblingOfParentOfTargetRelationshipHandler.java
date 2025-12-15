package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.PriorityLayer;

public class SiblingOfParentOfTargetRelationshipHandler extends SpiralSiblingRelationshipHandler {
  private final PriorityLayer priorityFourLayer;

  public SiblingOfParentOfTargetRelationshipHandler(
      Note relatingNote, PriorityLayer priorityFourLayer) {
    super(
        RelationshipToFocusNote.SiblingOfParentOfTarget,
        relatingNote,
        relatingNote.getTargetNote() != null ? relatingNote.getTargetNote().getParent() : null);
    this.priorityFourLayer = priorityFourLayer;
  }

  @Override
  public Note handle() {
    Note targetParentSibling = super.handle();
    if (targetParentSibling != null && priorityFourLayer != null) {
      priorityFourLayer.addHandler(
          new ChildOfSiblingOfParentOfTargetRelationshipHandler(targetParentSibling));
    }
    return targetParentSibling;
  }
}
