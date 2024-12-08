package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.PriorityLayer;

public class SubjectOfInboundReferenceRelationshipHandler extends RelationshipHandler {
  private final Note inboundReferenceNote;
  private boolean handled = false;
  private final PriorityLayer priorityFourLayer;

  public SubjectOfInboundReferenceRelationshipHandler(
      Note inboundReferenceNote, PriorityLayer priorityFourLayer) {
    super(RelationshipToFocusNote.SubjectOfInboundReference, inboundReferenceNote);
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
            new SiblingOfSubjectOfInboundReferenceRelationshipHandler(referringSubject));
      }

      return referringSubject;
    }
    return null;
  }
}
