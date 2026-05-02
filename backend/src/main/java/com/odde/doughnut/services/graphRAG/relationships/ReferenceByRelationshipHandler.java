package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;
import java.util.ArrayList;
import java.util.List;

public class ReferenceByRelationshipHandler extends RelationshipHandler {
  private final List<Note> inboundReferenceNotes;
  private int currentIndex = 0;

  public ReferenceByRelationshipHandler(List<Note> inboundReferenceNotes) {
    super(null, null);
    this.inboundReferenceNotes = new ArrayList<>(inboundReferenceNotes);
  }

  @Override
  public Note handle() {
    return consumeNextInboundReferrer();
  }

  /** Next inbound referrer; null when exhausted. */
  public Note consumeNextInboundReferrer() {
    if (currentIndex >= inboundReferenceNotes.size()) {
      return null;
    }
    return inboundReferenceNotes.get(currentIndex++);
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    List<String> refs = focus.getInboundReferences();
    if (!refs.contains(addedNote.getUri())) {
      refs.add(addedNote.getUri());
    }
  }
}
