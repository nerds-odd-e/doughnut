package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public abstract class RelationshipHandler {
  public abstract BareNote handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes);

  public void afterHandledSuccessfully(FocusNote focusNote, BareNote addedNote) {}
}
