package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.PriorityLayer;

public class TargetOfRelatedChildRelationshipHandler extends RelationshipHandler {
  private final Note targetNote;
  private boolean handled = false;
  private final PriorityLayer priorityFourLayer;

  public TargetOfRelatedChildRelationshipHandler(
      Note relatedChild, PriorityLayer priorityFourLayer) {
    super(RelationshipToFocusNote.TargetOfRelatedChild, relatedChild);
    this.targetNote = relatedChild.getTargetNote();
    this.priorityFourLayer = priorityFourLayer;
  }

  @Override
  public Note handle() {
    if (!handled && targetNote != null) {
      handled = true;
      if (priorityFourLayer != null) {
        priorityFourLayer.addHandler(new InboundReferenceToTargetOfRelatedChildHandler(targetNote));
      }
      return targetNote;
    }
    return null;
  }
}
