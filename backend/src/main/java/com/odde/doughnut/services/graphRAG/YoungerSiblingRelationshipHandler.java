package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.List;

public class YoungerSiblingRelationshipHandler extends RelationshipHandler {
  private final GraphRAGService graphRAGService;
  private int currentSiblingIndex = -1; // -1 means we haven't found focus note index yet

  public YoungerSiblingRelationshipHandler(GraphRAGService graphRAGService) {
    this.graphRAGService = graphRAGService;
  }

  @Override
  public void handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    if (focusNote.getParent() != null) {
      List<Note> siblings = focusNote.getSiblings();

      if (currentSiblingIndex == -1) {
        // First time: find focus note's index
        currentSiblingIndex = siblings.indexOf(focusNote) + 1;
      }

      if (currentSiblingIndex < siblings.size()) {
        Note youngerSibling = siblings.get(currentSiblingIndex);
        BareNote addedNote =
            graphRAGService.addNoteToRelatedNotes(
                relatedNotes, youngerSibling, RelationshipToFocusNote.YoungerSibling);

        if (addedNote != null) {
          focus.getYoungerSiblings().add(addedNote.getUriAndTitle());
        }

        currentSiblingIndex++;
        // Process next sibling before moving to next handler
        handle(focusNote, focus, relatedNotes);
      } else {
        // Reset for next use
        currentSiblingIndex = -1;
        handleNext(focusNote, focus, relatedNotes);
      }
    } else {
      handleNext(focusNote, focus, relatedNotes);
    }
  }
}
