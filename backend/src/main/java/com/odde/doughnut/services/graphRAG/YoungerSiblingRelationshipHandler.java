package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.List;

public class YoungerSiblingRelationshipHandler extends RelationshipHandler {
  private final GraphRAGService graphRAGService;
  private int currentSiblingIndex = -1; // -1 means we haven't found focus note index yet
  boolean exhausted = false;

  public YoungerSiblingRelationshipHandler(GraphRAGService graphRAGService) {
    super(RelationshipToFocusNote.YoungerSibling);
    this.graphRAGService = graphRAGService;
  }

  @Override
  public Note handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    if (exhausted) {
      return null;
    }
    if (focusNote.getParent() != null) {
      List<Note> siblings = focusNote.getSiblings();

      if (currentSiblingIndex == -1) {
        currentSiblingIndex = siblings.indexOf(focusNote) + 1;
      }

      if (currentSiblingIndex < siblings.size()) {
        currentSiblingIndex++;
        return siblings.get(currentSiblingIndex);

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
