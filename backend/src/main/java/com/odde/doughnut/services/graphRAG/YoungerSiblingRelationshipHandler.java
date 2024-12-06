package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class YoungerSiblingRelationshipHandler extends RelationshipHandler {
  private int currentSiblingIndex = -1;
  private boolean exhausted = false;
  private List<Note> siblings;

  public YoungerSiblingRelationshipHandler(Note relatingNote) {
    super(RelationshipToFocusNote.YoungerSibling, relatingNote);
  }

  @Override
  public Note handle() {
    if (exhausted) {
      return null;
    }

    if (relatingNote.getParent() != null) {
      if (siblings == null) {
        siblings = relatingNote.getSiblings();
      }

      if (currentSiblingIndex == -1) {
        currentSiblingIndex = siblings.indexOf(relatingNote) + 1;
      }

      if (currentSiblingIndex < siblings.size()) {
        return siblings.get(currentSiblingIndex++);
      } else {
        exhausted = true;
      }
    }
    return null;
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    focus.getYoungerSiblings().add(addedNote.getUriAndTitle());
  }
}
