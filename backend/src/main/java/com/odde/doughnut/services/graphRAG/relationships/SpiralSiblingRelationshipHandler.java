package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.List;

public abstract class SpiralSiblingRelationshipHandler extends RelationshipHandler {
  private final List<Note> siblings;
  private final int parentIndex;
  private int spiralStep = 1; // Start with 1 to get closest siblings first
  private boolean isLeft = true; // Start with left side

  protected SpiralSiblingRelationshipHandler(
      RelationshipToFocusNote relationship, Note relatingNote, Note parent) {
    super(relationship, relatingNote);
    if (parent != null) {
      this.siblings = parent.getSiblings();
      this.parentIndex = siblings.indexOf(parent);
    } else {
      this.siblings = List.of();
      this.parentIndex = -1;
    }
  }

  @Override
  public Note handle() {
    if (parentIndex < 0) return null;

    while (true) {
      int targetIndex = isLeft ? parentIndex - spiralStep : parentIndex + spiralStep;

      // Switch side for next time
      if (isLeft) {
        isLeft = false;
      } else {
        isLeft = true;
        spiralStep++; // Increase step after both sides are done
      }

      // Check if this index is valid
      if (targetIndex >= 0 && targetIndex < siblings.size() && targetIndex != parentIndex) {
        return siblings.get(targetIndex);
      }

      // If both this index and its opposite side would be invalid, we're done
      int oppositeIndex = isLeft ? parentIndex - spiralStep : parentIndex + spiralStep;
      if ((targetIndex < 0 || targetIndex >= siblings.size())
          && (oppositeIndex < 0 || oppositeIndex >= siblings.size())) {
        return null;
      }
    }
  }
}
