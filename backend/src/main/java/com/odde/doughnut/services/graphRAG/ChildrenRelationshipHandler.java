package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.List;

public class ChildrenRelationshipHandler extends RelationshipHandler {
  private final GraphRAGService graphRAGService;

  public ChildrenRelationshipHandler(GraphRAGService graphRAGService) {
    this.graphRAGService = graphRAGService;
  }

  @Override
  public void handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    List<Note> children = focusNote.getChildren();
    for (Note child : children) {
      BareNote addedNote =
          graphRAGService.addNoteToRelatedNotes(relatedNotes, child, RelationshipToFocusNote.Child);

      if (addedNote != null) {
        focus.getChildren().add(addedNote.getUriAndTitle());
      }
    }
    handleNext(focusNote, focus, relatedNotes);
  }
}
