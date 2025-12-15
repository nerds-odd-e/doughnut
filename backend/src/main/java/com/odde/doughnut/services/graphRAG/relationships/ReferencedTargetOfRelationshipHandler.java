package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReferencedTargetOfRelationshipHandler extends RelationshipHandler {
  private final List<Note> inboundReferences;
  private int currentIndex = 0;

  public ReferencedTargetOfRelationshipHandler(Note targetNote) {
    super(RelationshipToFocusNote.ReferencedTargetOfRelationship, targetNote);
    this.inboundReferences = new ArrayList<>(targetNote.getInboundReferences());
    Collections.shuffle(this.inboundReferences);
  }

  @Override
  public Note handle() {
    if (currentIndex < inboundReferences.size()) {
      return inboundReferences.get(currentIndex++);
    }
    return null;
  }
}
