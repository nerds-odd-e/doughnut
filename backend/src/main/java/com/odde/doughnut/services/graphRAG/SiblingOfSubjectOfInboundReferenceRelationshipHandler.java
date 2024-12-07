package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class SiblingOfSubjectOfInboundReferenceRelationshipHandler extends RelationshipHandler {
  private final List<Note> cousins;
  private int currentIndex = 0;

  public SiblingOfSubjectOfInboundReferenceRelationshipHandler(Note inboundReferenceSubject) {
    super(RelationshipToFocusNote.SiblingOfSubjectOfInboundReference, inboundReferenceSubject);
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
