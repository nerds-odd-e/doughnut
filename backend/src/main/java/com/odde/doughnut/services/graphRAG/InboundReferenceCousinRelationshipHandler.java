package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class InboundReferenceCousinRelationshipHandler extends RelationshipHandler {
  private final List<Note> cousins;
  private int currentIndex = 0;

  public InboundReferenceCousinRelationshipHandler(Note inboundReferenceSubject) {
    super(RelationshipToFocusNote.InboundReferenceCousin, inboundReferenceSubject);
    this.cousins = inboundReferenceSubject.getChildren();
  }

  @Override
  public Note handle() {
    if (currentIndex < cousins.size()) {
      return cousins.get(currentIndex++);
    }
    return null;
  }
}
