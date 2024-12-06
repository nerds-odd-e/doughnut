package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class ParentSiblingChildRelationshipHandler extends RelationshipHandler {
  private final List<Note> children;
  private int currentIndex = 0;

  public ParentSiblingChildRelationshipHandler(Note parentSibling) {
    super(RelationshipToFocusNote.ParentSiblingChild, parentSibling);
    this.children = parentSibling.getChildren();
  }

  @Override
  public Note handle() {
    if (currentIndex < children.size()) {
      return children.get(currentIndex++);
    }
    return null;
  }
}
