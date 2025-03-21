package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SiblingOfSubjectOfInboundReferenceRelationshipHandler extends RelationshipHandler {
  private final List<Note> cousins;
  private int currentIndex = 0;

  public SiblingOfSubjectOfInboundReferenceRelationshipHandler(Note inboundReferenceSubject) {
    super(RelationshipToFocusNote.SiblingOfSubjectOfInboundReference, inboundReferenceSubject);
    this.cousins = new ArrayList<>(inboundReferenceSubject.getChildren());
    Collections.shuffle(this.cousins);
  }

  @Override
  public Note handle() {
    if (currentIndex < cousins.size()) {
      return cousins.get(currentIndex++);
    }
    return null;
  }
}
