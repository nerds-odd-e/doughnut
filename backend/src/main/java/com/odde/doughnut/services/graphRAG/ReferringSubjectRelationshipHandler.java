package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class ReferringSubjectRelationshipHandler extends RelationshipHandler {
  private final Note referringNote;
  private boolean handled = false;

  public ReferringSubjectRelationshipHandler(Note referringNote) {
    super(RelationshipToFocusNote.ReferringSubject, referringNote);
    this.referringNote = referringNote;
  }

  @Override
  public Note handle() {
    if (!handled && referringNote.getParent() != null) {
      handled = true;
      return referringNote.getParent();
    }
    return null;
  }
}
