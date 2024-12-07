package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import lombok.Setter;

public class ObjectOfReifiedChildRelationshipHandler extends RelationshipHandler {
  private final Note targetNote;
  private boolean handled = false;
  @Setter private PriorityLayer priorityFourLayer;

  public ObjectOfReifiedChildRelationshipHandler(Note reifiedChild) {
    super(RelationshipToFocusNote.ObjectOfReifiedChild, reifiedChild);
    this.targetNote = reifiedChild.getTargetNote();
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
