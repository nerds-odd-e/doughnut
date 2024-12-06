package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;

public class ContextualPathRelationshipHandler extends RelationshipHandler {
  private final List<Note> ancestors;
  private int currentIndex = 0;

  public ContextualPathRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.NoteInContextualPath, relatingNote);
    ancestors = new ArrayList<>(relatingNote.getAncestors());
  }

  @Override
  public Note handle() {
    if (currentIndex < ancestors.size()) {
      return ancestors.get(currentIndex++);
    }
    return null;
  }
}
