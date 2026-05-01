package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.PriorityLayer;
import java.util.List;

public class TargetParentSiblingRelationshipHandler extends SpiralSiblingRelationshipHandler {
  private final PriorityLayer priorityFourLayer;

  public TargetParentSiblingRelationshipHandler(
      Note relatingNote,
      PriorityLayer priorityFourLayer,
      Note targetParent,
      List<Note> targetParentStructuralPeers) {
    super(
        RelationshipToFocusNote.TargetParentSibling,
        relatingNote,
        targetParent,
        targetParentStructuralPeers);
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
