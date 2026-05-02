package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;

public class SiblingOfTargetRelationshipHandler extends RelationshipHandler {
  private final List<Note> targetSiblings;
  private int currentIndex = 0;

  public SiblingOfTargetRelationshipHandler(List<Note> targetSiblings) {
    super(null, null);
    this.targetSiblings = new ArrayList<>(targetSiblings);
  }

  @Override
  public Note handle() {
    if (currentIndex < targetSiblings.size()) {
      return targetSiblings.get(currentIndex++);
    }
    return null;
  }
}
