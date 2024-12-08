package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.PriorityLayer;

public class ObjectOfReifiedChildRelationshipHandler extends RelationshipHandler {
  private final Note targetNote;
  private boolean handled = false;
  private final PriorityLayer priorityFourLayer;

  public ObjectOfReifiedChildRelationshipHandler(
      Note reifiedChild, PriorityLayer priorityFourLayer) {
    super(RelationshipToFocusNote.ObjectOfReifiedChild, reifiedChild);
    this.targetNote = reifiedChild.getTargetNote();
    this.priorityFourLayer = priorityFourLayer;
  }

  @Override
  public Note handle() {
    if (!handled && targetNote != null) {
      handled = true;
      if (priorityFourLayer != null) {
        priorityFourLayer.addHandler(new InboundReferenceToObjectOfReifiedChildHandler(targetNote));
      }
      return targetNote;
    }
    return null;
  }
}
