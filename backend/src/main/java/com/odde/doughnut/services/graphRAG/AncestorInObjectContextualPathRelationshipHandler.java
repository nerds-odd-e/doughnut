package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class AncestorInObjectContextualPathRelationshipHandler extends RelationshipHandler {
  private final List<Note> objectContextualPath;
  private int currentIndex = 0;

  public AncestorInObjectContextualPathRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.AncestorInObjectContextualPath, relatingNote);
    Note objectNote = relatingNote.getTargetNote();
    this.objectContextualPath = objectNote != null ? objectNote.getAncestors() : List.of();
  }

  @Override
  public Note handle() {
    if (currentIndex < objectContextualPath.size()) {
      return objectContextualPath.get(currentIndex++);
    }
    return null;
  }
}
