package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InboundReferenceToObjectOfReifiedChildHandler extends RelationshipHandler {
  private final List<Note> inboundReferences;
  private int currentIndex = 0;

  public InboundReferenceToObjectOfReifiedChildHandler(Note objectNote) {
    super(RelationshipToFocusNote.InboundReferenceToObjectOfReifiedChild, objectNote);
    this.inboundReferences = new ArrayList<>(objectNote.getInboundReferences());
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
