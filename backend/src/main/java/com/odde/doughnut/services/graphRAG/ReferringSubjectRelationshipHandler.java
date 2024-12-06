package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class ReferringSubjectRelationshipHandler extends RelationshipHandler {
  private final Note referringNote;
  private boolean handled = false;
  private final PriorityLayer priorityFourLayer;

  public ReferringSubjectRelationshipHandler(Note referringNote, PriorityLayer priorityFourLayer) {
    super(RelationshipToFocusNote.ReferringSubject, referringNote);
    this.referringNote = referringNote;
    this.priorityFourLayer = priorityFourLayer;
  }

  @Override
  public Note handle() {
    if (!handled && referringNote.getParent() != null) {
      handled = true;
      Note referringSubject = referringNote.getParent();

      // Push cousins (children of referring subject) to layer 4
      if (priorityFourLayer != null) {
        priorityFourLayer.addHandler(new ReferringCousinRelationshipHandler(referringSubject));
      }

      return referringSubject;
    }
    return null;
  }
}
