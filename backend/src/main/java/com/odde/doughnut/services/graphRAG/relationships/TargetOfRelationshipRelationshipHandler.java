package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.PriorityLayer;

public class TargetOfRelationshipRelationshipHandler extends RelationshipHandler {
  private final Note targetNote;
  private boolean handled = false;
  private final PriorityLayer priorityFourLayer;

  public TargetOfRelationshipRelationshipHandler(
      Note relatedChild, PriorityLayer priorityFourLayer) {
    super(RelationshipToFocusNote.TargetOfRelationship, relatedChild);
    this.targetNote = relatedChild.getTargetNote();
    this.priorityFourLayer = priorityFourLayer;
  }

  @Override
  public Note handle() {
    if (!handled && targetNote != null) {
      handled = true;
      if (priorityFourLayer != null) {
        priorityFourLayer.addHandler(new ReferencedTargetOfRelationshipHandler(targetNote));
      }
      return targetNote;
    }
    return null;
  }
}
