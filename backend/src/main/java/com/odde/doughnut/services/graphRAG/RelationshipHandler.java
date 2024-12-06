package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public abstract class RelationshipHandler {
  protected RelationshipHandler next;

  public void setNext(RelationshipHandler next) {
    this.next = next;
  }

  public abstract void handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes);

  protected void handleNext(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    if (next != null) {
      next.handle(focusNote, focus, relatedNotes);
    }
  }
}
