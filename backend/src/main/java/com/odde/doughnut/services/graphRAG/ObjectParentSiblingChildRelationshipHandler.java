package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class ObjectParentSiblingChildRelationshipHandler extends RelationshipHandler {
  private final List<Note> children;
  private int currentIndex = 0;

  public ObjectParentSiblingChildRelationshipHandler(Note objectParentSibling) {
    super(RelationshipToFocusNote.ObjectParentSiblingChild, objectParentSibling);
    this.children = objectParentSibling.getChildren();
  }

  @Override
  public Note handle() {
    if (currentIndex < children.size()) {
      return children.get(currentIndex++);
    }
    return null;
  }
}
