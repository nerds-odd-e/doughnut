package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InboundReferenceRelationshipHandler extends RelationshipHandler {
  private final List<Note> inboundReferenceNotes;
  private int currentIndex = 0;
  private final PriorityLayer priorityThreeLayer;
  private final PriorityLayer priorityFourLayer;

  public InboundReferenceRelationshipHandler(
      Note relatingNote, PriorityLayer priorityThreeLayer, PriorityLayer priorityFourLayer) {
    super(RelationshipToFocusNote.InboundReference, relatingNote);
    this.inboundReferenceNotes = new ArrayList<>(relatingNote.getInboundReferences());
    Collections.shuffle(this.inboundReferenceNotes);
    this.priorityThreeLayer = priorityThreeLayer;
    this.priorityFourLayer = priorityFourLayer;
  }

  @Override
  public Note handle() {
    if (currentIndex < inboundReferenceNotes.size()) {
      Note referringNote = inboundReferenceNotes.get(currentIndex++);

      // Add referring subject to priority 3
      if (priorityThreeLayer != null) {
        priorityThreeLayer.addHandler(
            new SubjectOfInboundReferenceRelationshipHandler(referringNote, priorityFourLayer));
      }

      // Add referring contextual path to priority 4
      if (priorityFourLayer != null) {
        priorityFourLayer.addHandler(
            new InboundReferenceContextualPathRelationshipHandler(referringNote));
      }

      return referringNote;
    }
    return null;
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    focus.getInboundReferences().add(addedNote.getUri());
  }
}
