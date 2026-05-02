package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;

public class TargetRelationshipHandler extends RelationshipHandler {
  private boolean exhausted = false;
  private final Note target;

  public TargetRelationshipHandler(Note target) {
    super(RelationshipToFocusNote.RelationshipTarget, target);
    this.target = target;
  }

  @Override
  public Note handle() {
    if (exhausted) {
      return null;
    }
    exhausted = true;
    return target;
  }

  @Override
  public RelationshipToFocusNote getRelationshipToFocusNoteFor(Note note) {
    return null;
  }

  @Override
  public boolean isLinkFromFocusFor(Note note) {
    return target != null && note != null && note.getId().equals(target.getId());
  }
}
