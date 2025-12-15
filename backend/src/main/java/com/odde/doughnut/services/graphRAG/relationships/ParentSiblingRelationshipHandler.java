package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.PriorityLayer;

public class ParentSiblingRelationshipHandler extends SpiralSiblingRelationshipHandler {
  private final PriorityLayer priorityFourLayer;

  public ParentSiblingRelationshipHandler(Note relatingNote, PriorityLayer priorityFourLayer) {
    super(RelationshipToFocusNote.ParentSibling, relatingNote, relatingNote.getParent());
    this.priorityFourLayer = priorityFourLayer;
  }

  @Override
  public Note handle() {
    Note parentSibling = super.handle();
    if (parentSibling != null && priorityFourLayer != null) {
      priorityFourLayer.addHandler(new ParentSiblingChildRelationshipHandler(parentSibling));
    }
    return parentSibling;
  }
}
