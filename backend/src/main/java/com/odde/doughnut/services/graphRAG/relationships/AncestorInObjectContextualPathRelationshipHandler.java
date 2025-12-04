package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class AncestorInObjectContextualPathRelationshipHandler extends RelationshipHandler {
  private final List<Note> objectContextualPath;
  private int currentIndex;

  public AncestorInObjectContextualPathRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.AncestorInObjectContextualPath, relatingNote);
    Note objectNote = relatingNote.getTargetNote();
    this.objectContextualPath = objectNote != null ? objectNote.getAncestors() : List.of();
    // Start from the end (closest ancestor/parent) and work backwards to root
    this.currentIndex = objectContextualPath.size() - 1;
  }

  @Override
  public Note handle() {
    if (currentIndex >= 0) {
      return objectContextualPath.get(currentIndex--);
    }
    return null;
  }
}
