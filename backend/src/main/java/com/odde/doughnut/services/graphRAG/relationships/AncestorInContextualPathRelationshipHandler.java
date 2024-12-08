package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;

public class AncestorInContextualPathRelationshipHandler extends RelationshipHandler {
  private final List<Note> ancestors;
  private int currentIndex = 0;

  public AncestorInContextualPathRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.AncestorInContextualPath, relatingNote);
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
