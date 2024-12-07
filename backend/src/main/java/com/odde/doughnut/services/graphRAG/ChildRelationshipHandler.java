package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class ChildRelationshipHandler extends RelationshipHandler {
  private int currentChildIndex = 0;
  private final List<Note> children;
  private final PriorityLayer priorityThreeLayer;

  public ChildRelationshipHandler(Note relatingNote, PriorityLayer priorityThreeLayer) {
    super(RelationshipToFocusNote.Child, relatingNote);
    this.children = relatingNote.getChildren();
    this.priorityThreeLayer = priorityThreeLayer;
  }

  @Override
  public Note handle() {
    if (currentChildIndex < children.size()) {
      Note child = children.get(currentChildIndex++);

      // If this child is a reified note, add its object to priority 3
      if (child.getTargetNote() != null && priorityThreeLayer != null) {
        priorityThreeLayer.addHandler(new ObjectOfReifiedChildRelationshipHandler(child));
      }

      return child;
    }
    return null;
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    focus.getChildren().add(addedNote.getUriAndTitle());
  }
}
