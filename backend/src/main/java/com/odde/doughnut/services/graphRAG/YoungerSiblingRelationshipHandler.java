package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.List;

public class YoungerSiblingRelationshipHandler extends RelationshipHandler {
  private final GraphRAGService graphRAGService;

  public YoungerSiblingRelationshipHandler(GraphRAGService graphRAGService) {
    this.graphRAGService = graphRAGService;
  }

  @Override
  public void handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes, int budget) {
    if (focusNote.getParent() != null) {
      List<Note> siblings = focusNote.getSiblings();
      int focusIndex = siblings.indexOf(focusNote);

      for (int i = focusIndex + 1; i < siblings.size(); i++) {
        Note youngerSibling = siblings.get(i);
        BareNote addedNote =
            graphRAGService.addNoteToRelatedNotes(
                relatedNotes, youngerSibling, RelationshipToFocusNote.YoungerSibling, budget);

        if (addedNote != null) {
          focus.getYoungerSiblings().add(addedNote.getUriAndTitle());
          budget -= addedNote.getDetails().length() / GraphRAGService.CHARACTERS_PER_TOKEN;
        }
      }
    }
    handleNext(focusNote, focus, relatedNotes, budget);
  }
}
