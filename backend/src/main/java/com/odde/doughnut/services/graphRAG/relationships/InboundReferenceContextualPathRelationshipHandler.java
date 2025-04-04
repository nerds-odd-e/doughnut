package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InboundReferenceContextualPathRelationshipHandler extends RelationshipHandler {
  private final List<Note> contextualPath;
  private int currentIndex = 0;

  public InboundReferenceContextualPathRelationshipHandler(Note inboundReferenceNote) {
    super(RelationshipToFocusNote.InboundReferenceContextualPath, inboundReferenceNote);
    this.contextualPath = new ArrayList<>(inboundReferenceNote.getAncestors());
    Collections.shuffle(this.contextualPath);
  }

  @Override
  public Note handle() {
    if (currentIndex < contextualPath.size()) {
      return contextualPath.get(currentIndex++);
    }
    return null;
  }
}
