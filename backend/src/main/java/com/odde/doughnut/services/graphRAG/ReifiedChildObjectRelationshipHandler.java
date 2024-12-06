package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class ReifiedChildObjectRelationshipHandler extends RelationshipHandler {
  private final Note targetNote;
  private boolean handled = false;

  public ReifiedChildObjectRelationshipHandler(Note reifiedChild) {
    super(RelationshipToFocusNote.ReifiedChildObject, reifiedChild);
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
