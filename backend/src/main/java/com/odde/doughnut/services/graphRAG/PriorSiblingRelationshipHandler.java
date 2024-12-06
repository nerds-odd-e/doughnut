package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class PriorSiblingRelationshipHandler extends RelationshipHandler {
  private final List<Note> siblings;
  private int currentIndex;

  public PriorSiblingRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.PriorSibling, relatingNote);
    siblings = relatingNote.getSiblings();
    currentIndex = 0;
  }

  @Override
  public Note handle() {
    int focusIndex = siblings.indexOf(relatingNote);
    if (currentIndex < focusIndex) {
      return siblings.get(currentIndex++);
    }
    return null;
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    focus.getPriorSiblings().add(addedNote.getUriAndTitle());
  }
}
