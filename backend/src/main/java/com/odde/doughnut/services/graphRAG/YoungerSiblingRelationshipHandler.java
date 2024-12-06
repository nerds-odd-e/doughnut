package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.List;

public class YoungerSiblingRelationshipHandler extends RelationshipHandler {
  private final GraphRAGService graphRAGService;
  private int currentSiblingIndex = -1; // -1 means we haven't found focus note index yet
  boolean exhausted = false;

  public YoungerSiblingRelationshipHandler(GraphRAGService graphRAGService) {
    this.graphRAGService = graphRAGService;
  }

  @Override
  public BareNote handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    if (exhausted) {
      return null;
    }
    if (focusNote.getParent() != null) {
      List<Note> siblings = focusNote.getSiblings();

      if (currentSiblingIndex == -1) {
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
        return addedNote;
      } else {
        exhausted = true;
      }
    }
    return null;
  }
}
