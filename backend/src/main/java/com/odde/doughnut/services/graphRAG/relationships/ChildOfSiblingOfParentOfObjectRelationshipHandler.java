package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class ChildOfSiblingOfParentOfObjectRelationshipHandler extends RelationshipHandler {
  private final List<Note> children;
  private int currentIndex = 0;

  public ChildOfSiblingOfParentOfObjectRelationshipHandler(Note objectParentSibling) {
    super(RelationshipToFocusNote.ChildOfSiblingOfParentOfObject, objectParentSibling);
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
