package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import lombok.Getter;

public abstract class RelationshipHandler {
  @Getter protected final RelationshipToFocusNote relationshipToFocusNote;

  protected RelationshipHandler(RelationshipToFocusNote relationshipToFocusNote) {
    this.relationshipToFocusNote = relationshipToFocusNote;
  }

  public abstract Note handle(Note focusNote);

  public void afterHandledSuccessfully(FocusNote focusNote, BareNote addedNote) {}
}
