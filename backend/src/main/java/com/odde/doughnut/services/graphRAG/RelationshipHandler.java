package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;
import lombok.Getter;

public abstract class RelationshipHandler {
  @Getter protected final RelationshipToFocusNote relationshipToFocusNote;

  protected RelationshipHandler(RelationshipToFocusNote relationshipToFocusNote) {
    this.relationshipToFocusNote = relationshipToFocusNote;
  }

  public abstract BareNote handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes);

  public void afterHandledSuccessfully(FocusNote focusNote, BareNote addedNote) {}
}
