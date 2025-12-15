package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;

public class SubjectOfTargetSiblingRelationshipHandler extends RelationshipHandler {
  private final Note targetSiblingNote;
  private boolean handled = false;

  public SubjectOfTargetSiblingRelationshipHandler(Note targetSiblingNote) {
    super(RelationshipToFocusNote.SubjectOfTargetSibling, targetSiblingNote);
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
