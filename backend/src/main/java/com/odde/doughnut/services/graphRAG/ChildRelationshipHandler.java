package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class ChildRelationshipHandler extends RelationshipHandler {
  private int currentChildIndex = 0;
  private boolean exhausted = false;

  public ChildRelationshipHandler() {
    super(RelationshipToFocusNote.Child);
  }

  @Override
  public Note handle(Note focusNote) {
    if (exhausted) {
      return null;
    }
    List<Note> children = focusNote.getChildren();

    if (currentChildIndex < children.size()) {
      return children.get(currentChildIndex++);
    } else {
      exhausted = true;
    }
    return null;
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    focus.getChildren().add(addedNote.getUriAndTitle());
  }
}
