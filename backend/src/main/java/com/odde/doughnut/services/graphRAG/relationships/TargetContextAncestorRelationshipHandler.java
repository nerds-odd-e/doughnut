package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class TargetContextAncestorRelationshipHandler extends RelationshipHandler {
  private final List<Note> targetContextualPath;
  private int currentIndex;

  public TargetContextAncestorRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.TargetContextAncestor, relatingNote);
    Note targetNote = relatingNote.getTargetNote();
    this.targetContextualPath = targetNote != null ? targetNote.getAncestors() : List.of();
    // Start from the end (closest ancestor/parent) and work backwards to root
    this.currentIndex = targetContextualPath.size() - 1;
  }

  @Override
  public Note handle() {
    if (currentIndex >= 0) {
      return targetContextualPath.get(currentIndex--);
    }
    return null;
  }
}
