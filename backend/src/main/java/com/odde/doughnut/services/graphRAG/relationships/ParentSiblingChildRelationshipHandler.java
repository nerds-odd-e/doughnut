package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParentSiblingChildRelationshipHandler extends RelationshipHandler {
  private final List<Note> children;
  private int currentIndex = 0;

  public ParentSiblingChildRelationshipHandler(Note parentSibling) {
    super(RelationshipToFocusNote.ParentSiblingChild, parentSibling);
    this.children = new ArrayList<>(parentSibling.getChildren());
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
