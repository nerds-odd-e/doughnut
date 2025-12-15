package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.PriorityLayer;

public class ReferencingNoteRelationshipHandler extends RelationshipHandler {
  private final Note inboundReferenceNote;
  private boolean handled = false;
  private final PriorityLayer priorityFourLayer;

  public ReferencingNoteRelationshipHandler(
      Note inboundReferenceNote, PriorityLayer priorityFourLayer) {
    super(RelationshipToFocusNote.ReferencingNote, inboundReferenceNote);
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
            new SiblingOfReferencingNoteRelationshipHandler(referringSubject));
      }

      return referringSubject;
    }
    return null;
  }
}
