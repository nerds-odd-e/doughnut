package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;

public class ReifiedChildObjectRelationshipHandler extends RelationshipHandler {
  private final List<Note> childObjects = new ArrayList<>();
  private int currentIndex = 0;

  public ReifiedChildObjectRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.ReifiedChildObject, relatingNote);
    // Find all children that are links
    for (Note child : relatingNote.getChildren()) {
      if (child.getTargetNote() != null) {
        childObjects.add(child.getTargetNote());
      }
    }
  }

  @Override
  public Note handle() {
    if (currentIndex < childObjects.size()) {
      return childObjects.get(currentIndex++);
    }
    return null;
  }
}
