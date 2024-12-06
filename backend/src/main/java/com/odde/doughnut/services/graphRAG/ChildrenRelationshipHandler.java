package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.List;

public class ChildrenRelationshipHandler extends RelationshipHandler {
  private final GraphRAGService graphRAGService;
  private int currentChildIndex = 0;

  public ChildrenRelationshipHandler(GraphRAGService graphRAGService) {
    this.graphRAGService = graphRAGService;
  }

  @Override
  public void handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    List<Note> children = focusNote.getChildren();

    if (currentChildIndex < children.size()) {
      Note child = children.get(currentChildIndex);
      BareNote addedNote =
          graphRAGService.addNoteToRelatedNotes(relatedNotes, child, RelationshipToFocusNote.Child);

      if (addedNote != null) {
        focus.getChildren().add(addedNote.getUriAndTitle());
      }

      currentChildIndex++;
      // Process next child before moving to next handler
      handle(focusNote, focus, relatedNotes);
    } else {
      // Reset for next use
      currentChildIndex = 0;
      handleNext(focusNote, focus, relatedNotes);
    }
  }
}
