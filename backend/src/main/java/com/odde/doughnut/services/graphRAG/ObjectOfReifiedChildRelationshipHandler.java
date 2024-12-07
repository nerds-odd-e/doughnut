package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class ObjectOfReifiedChildRelationshipHandler extends RelationshipHandler {
  private final Note targetNote;
  private boolean handled = false;

  public ObjectOfReifiedChildRelationshipHandler(Note reifiedChild) {
    super(RelationshipToFocusNote.ObjectOfReifiedChild, reifiedChild);
    this.targetNote = reifiedChild.getTargetNote();
  }

  @Override
  public Note handle() {
    if (!handled && targetNote != null) {
      handled = true;
      return targetNote;
    }
    return null;
  }
}
