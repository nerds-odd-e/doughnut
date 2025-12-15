package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChildOfSiblingOfParentOfTargetRelationshipHandler extends RelationshipHandler {
  private final List<Note> children;
  private int currentIndex = 0;

  public ChildOfSiblingOfParentOfTargetRelationshipHandler(Note targetParentSibling) {
    super(RelationshipToFocusNote.ChildOfSiblingOfParentOfTarget, targetParentSibling);
    this.children = new ArrayList<>(targetParentSibling.getChildren());
    Collections.shuffle(this.children);
  }

  @Override
  public Note handle() {
    if (currentIndex < children.size()) {
      return children.get(currentIndex++);
    }
    return null;
  }
}
