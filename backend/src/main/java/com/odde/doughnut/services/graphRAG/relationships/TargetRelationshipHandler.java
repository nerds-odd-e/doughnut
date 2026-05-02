package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;

public class TargetRelationshipHandler extends RelationshipHandler {
  private boolean exhausted = false;

  public TargetRelationshipHandler(Note target) {
    super(null, target);
  }

  @Override
  public Note handle() {
    if (exhausted || relatingNote == null) {
      return null;
    }
    exhausted = true;
    return relatingNote;
  }

  @Override
  public boolean isLinkFromFocusFor(Note note) {
    return relatingNote != null && note != null && note.getId().equals(relatingNote.getId());
  }
}
