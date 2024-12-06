package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;

public class ContextualPathRelationshipHandler extends RelationshipHandler {
  private List<Note> ancestors;
  private int currentIndex = 0;

  public ContextualPathRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.NoteInContextualPath, relatingNote);
  }

  @Override
  public Note handle() {
    if (ancestors == null) {
      ancestors = new ArrayList<>(relatingNote.getAncestors());
      // Remove immediate parent as it's handled by ParentRelationshipHandler
      if (!ancestors.isEmpty()) {
        ancestors.remove(ancestors.size() - 1);
      }
    }

    if (currentIndex < ancestors.size()) {
      return ancestors.get(currentIndex++);
    }

    return null;
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    // Add to beginning since we're processing from root to parent
    focus.getContextualPath().add(0, addedNote.getUriAndTitle());
  }
}
