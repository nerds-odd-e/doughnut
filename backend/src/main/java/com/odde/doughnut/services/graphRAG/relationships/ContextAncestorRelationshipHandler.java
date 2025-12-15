package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;

public class ContextAncestorRelationshipHandler extends RelationshipHandler {
  private final List<Note> ancestors;
  private int currentIndex;

  public ContextAncestorRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.ContextAncestor, relatingNote);
    ancestors = new ArrayList<>(relatingNote.getAncestors());
    // Start from the end (closest ancestor/parent) and work backwards to root
    this.currentIndex = ancestors.size() - 1;
  }

  @Override
  public Note handle() {
    if (currentIndex >= 0) {
      return ancestors.get(currentIndex--);
    }
    return null;
  }
}
