package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;

public class OutgoingWikiLinkRelationshipHandler extends RelationshipHandler
    implements PollableNoteRelationshipHandler {
  private final List<Note> outgoingTargetNotes;
  private int currentIndex = 0;

  public OutgoingWikiLinkRelationshipHandler(List<Note> outgoingTargetNotes) {
    super(RelationshipToFocusNote.OutboundWikiLink, null);
    this.outgoingTargetNotes = new ArrayList<>(outgoingTargetNotes);
  }

  @Override
  public Note handle() {
    return pollNext();
  }

  @Override
  public Note pollNext() {
    if (currentIndex >= outgoingTargetNotes.size()) {
      return null;
    }
    return outgoingTargetNotes.get(currentIndex++);
  }
}
