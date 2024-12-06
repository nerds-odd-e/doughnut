package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class ParentSiblingRelationshipHandler extends RelationshipHandler {
  private final List<Note> parentSiblings;
  private int currentIndex = 0;

  public ParentSiblingRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.ParentSibling, relatingNote);
    Note parent = relatingNote.getParent();
    this.parentSiblings = parent != null ? parent.getSiblings() : List.of();
  }

  @Override
  public Note handle() {
    if (currentIndex < parentSiblings.size()) {
      return parentSiblings.get(currentIndex++);
    }
    return null;
  }
}
