package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class ChildRelationshipHandler extends RelationshipHandler {
  private int currentChildIndex = 0;
  private final List<Note> children;

  public ChildRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.Child, relatingNote);
    children = relatingNote.getChildren();
  }

  @Override
  public Note handle() {
    if (currentChildIndex < children.size()) {
      return children.get(currentChildIndex++);
    }
    return null;
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    focus.getChildren().add(addedNote.getUriAndTitle());
  }
}
