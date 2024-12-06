package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.List;

public class ChildrenRelationshipHandler extends RelationshipHandler {
  private final GraphRAGService graphRAGService;
  private int currentChildIndex = 0;
  private boolean exhausted = false;

  public ChildrenRelationshipHandler(GraphRAGService graphRAGService) {
    this.graphRAGService = graphRAGService;
  }

  @Override
  public BareNote handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    if (exhausted) {
      return null;
    }
    List<Note> children = focusNote.getChildren();

    if (currentChildIndex < children.size()) {
      Note child = children.get(currentChildIndex);
      BareNote addedNote =
          graphRAGService.addNoteToRelatedNotes(relatedNotes, child, RelationshipToFocusNote.Child);

      if (addedNote != null) {
        focus.getChildren().add(addedNote.getUriAndTitle());
      }

      currentChildIndex++;
      return addedNote;
    } else {
      exhausted = true;
    }
    return null;
  }
}
