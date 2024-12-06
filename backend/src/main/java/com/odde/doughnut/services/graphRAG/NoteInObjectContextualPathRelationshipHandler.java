package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class NoteInObjectContextualPathRelationshipHandler extends RelationshipHandler {
  private final List<Note> objectContextualPath;
  private int currentIndex = 0;

  public NoteInObjectContextualPathRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.NoteInObjectContextualPath, relatingNote);
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
