package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;

public class ReferenceByContextualPathRelationshipHandler extends RelationshipHandler {
  private final List<Note> contextualPath;
  private int currentIndex;

  public ReferenceByContextualPathRelationshipHandler(Note inboundReferenceNote) {
    super(RelationshipToFocusNote.ReferenceByContextualPath, inboundReferenceNote);
    this.contextualPath = new ArrayList<>(inboundReferenceNote.getAncestors());
    // Start from the end (closest ancestor/parent) and work backwards to root
    this.currentIndex = contextualPath.size() - 1;
  }

  @Override
  public Note handle() {
    if (currentIndex >= 0) {
      return contextualPath.get(currentIndex--);
    }
    return null;
  }
}
