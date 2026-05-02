package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.BareNote;
import com.odde.doughnut.services.graphRAG.FocusNote;
import lombok.Getter;

public abstract class RelationshipHandler {
  @Getter protected final RelationshipToFocusNote relationshipToFocusNote;
  protected final Note relatingNote;

  protected RelationshipHandler(
      RelationshipToFocusNote relationshipToFocusNote, Note relatingNote) {
    this.relationshipToFocusNote = relationshipToFocusNote;
    this.relatingNote = relatingNote;
  }

  public abstract Note handle();

  public RelationshipToFocusNote getRelationshipToFocusNoteFor(Note note) {
    return relationshipToFocusNote;
  }

  public boolean isLinkFromFocusFor(Note note) {
    return false;
  }

  public boolean isLinkHop2For(Note note) {
    return false;
  }

  public void afterHandledSuccessfully(FocusNote focusNote, BareNote addedNote) {}
}
