package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;

public class SubjectOfObjectSiblingRelationshipHandler extends RelationshipHandler {
  private final Note objectSiblingNote;
  private boolean handled = false;

  public SubjectOfObjectSiblingRelationshipHandler(Note objectSiblingNote) {
    super(RelationshipToFocusNote.SubjectOfObjectSibling, objectSiblingNote);
    this.objectSiblingNote = objectSiblingNote;
  }

  @Override
  public Note handle() {
    if (!handled && objectSiblingNote.getParent() != null) {
      handled = true;
      return objectSiblingNote.getParent();
    }
    return null;
  }
}
