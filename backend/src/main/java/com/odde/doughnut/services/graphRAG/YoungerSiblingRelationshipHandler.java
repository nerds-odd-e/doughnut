package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class YoungerSiblingRelationshipHandler extends RelationshipHandler {
  private int currentSiblingIndex;
  private final List<Note> siblings;

  public YoungerSiblingRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.YoungerSibling, relatingNote);
    siblings = relatingNote.getSiblings();
    currentSiblingIndex = siblings.indexOf(relatingNote) + 1;
  }

  @Override
  public Note handle() {
    if (currentSiblingIndex < siblings.size()) {
      return siblings.get(currentSiblingIndex++);
    }
    return null;
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    focus.getYoungerSiblings().add(addedNote.getUriAndTitle());
  }
}
