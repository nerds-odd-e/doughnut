package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class SiblingOfParentRelationshipHandler extends SpiralSiblingRelationshipHandler {
  private final PriorityLayer priorityFourLayer;

  public SiblingOfParentRelationshipHandler(Note relatingNote, PriorityLayer priorityFourLayer) {
    super(RelationshipToFocusNote.SiblingOfParent, relatingNote, relatingNote.getParent());
    this.priorityFourLayer = priorityFourLayer;
  }

  @Override
  public Note handle() {
    Note parentSibling = super.handle();
    if (parentSibling != null && priorityFourLayer != null) {
      priorityFourLayer.addHandler(new ChildOfSiblingOfParentRelationshipHandler(parentSibling));
    }
    return parentSibling;
  }
}
