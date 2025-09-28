package com.odde.doughnut.services.graphRAG.relationships;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.BareNote;
import com.odde.doughnut.services.graphRAG.FocusNote;
import java.util.List;

public class PriorSiblingRelationshipHandler extends RelationshipHandler {
  private final List<Note> siblings;
  private int currentIndex;

  public PriorSiblingRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.PriorSibling, relatingNote);
    siblings = relatingNote.getSiblings();
    int focusIndex = siblings.indexOf(relatingNote);
    currentIndex = focusIndex - 1; // Start from newest prior sibling
  }

  @Override
  public Note handle() {
    if (currentIndex >= 0) { // Process prior siblings from newest to oldest
      return siblings.get(currentIndex--);
    }
    return null;
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    // Add to back to maintain creation order (oldest to newest)
    focus.getPriorSiblings().add(0, addedNote.getUriAndTitle());
  }
}
