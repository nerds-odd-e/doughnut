package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class InboundReferenceSubjectRelationshipHandler extends RelationshipHandler {
  private final Note inboundReferenceNote;
  private boolean handled = false;
  private final PriorityLayer priorityFourLayer;

  public InboundReferenceSubjectRelationshipHandler(
      Note inboundReferenceNote, PriorityLayer priorityFourLayer) {
    super(RelationshipToFocusNote.InboundReferenceSubject, inboundReferenceNote);
    this.inboundReferenceNote = inboundReferenceNote;
    this.priorityFourLayer = priorityFourLayer;
  }

  @Override
  public Note handle() {
    if (!handled && inboundReferenceNote.getParent() != null) {
      handled = true;
      Note referringSubject = inboundReferenceNote.getParent();

      // Push cousins (children of referring subject) to layer 4
      if (priorityFourLayer != null) {
        priorityFourLayer.addHandler(
            new InboundReferenceCousinRelationshipHandler(referringSubject));
      }

      return referringSubject;
    }
    return null;
  }
}
