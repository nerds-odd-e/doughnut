package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.PriorityLayer;
import java.util.ArrayList;
import java.util.List;

public class SiblingOfTargetRelationshipHandler extends RelationshipHandler {
  private final List<Note> targetSiblings;
  private int currentIndex = 0;
  private final PriorityLayer priorityThreeLayer;

  public SiblingOfTargetRelationshipHandler(
      List<Note> targetSiblings, PriorityLayer priorityThreeLayer) {
    super(RelationshipToFocusNote.SiblingOfTarget, null);
    this.priorityThreeLayer = priorityThreeLayer;
    this.targetSiblings = new ArrayList<>(targetSiblings);
  }

  @Override
  public Note handle() {
    if (currentIndex < targetSiblings.size()) {
      Note targetSibling = targetSiblings.get(currentIndex++);

      // Add subject of target sibling to priority 3
      if (priorityThreeLayer != null) {
        priorityThreeLayer.addHandler(
            new RelationshipOfTargetSiblingRelationshipHandler(targetSibling));
      }

      return targetSibling;
    }
    return null;
  }
}
