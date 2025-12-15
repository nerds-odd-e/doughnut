package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;

public class RelationshipOfTargetSiblingRelationshipHandler extends RelationshipHandler {
  private final Note targetSiblingNote;
  private boolean handled = false;

  public RelationshipOfTargetSiblingRelationshipHandler(Note targetSiblingNote) {
    super(RelationshipToFocusNote.RelationshipOfTargetSibling, targetSiblingNote);
    this.targetSiblingNote = targetSiblingNote;
  }

  @Override
  public Note handle() {
    if (!handled && targetSiblingNote.getParent() != null) {
      handled = true;
      return targetSiblingNote.getParent();
    }
    return null;
  }
}
