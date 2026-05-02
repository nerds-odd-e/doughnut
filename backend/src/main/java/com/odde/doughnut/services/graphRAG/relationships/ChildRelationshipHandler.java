package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;
import java.util.List;

public class ChildRelationshipHandler extends RelationshipHandler {
  private int currentChildIndex = 0;
  private final List<Note> children;

  public ChildRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.Child, relatingNote);
    this.children = relatingNote.getChildren();
  }

  @Override
  public Note handle() {
    while (currentChildIndex < children.size()) {
      Note child = children.get(currentChildIndex++);
      if (child.isRelation()) {
        continue;
      }
      return child;
    }
    return null;
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    focus.getChildren().add(addedNote.getUri());
  }
}
