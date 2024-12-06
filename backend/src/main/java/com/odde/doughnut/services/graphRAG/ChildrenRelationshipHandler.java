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
  public void handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes, int budget) {
    List<Note> children = focusNote.getChildren();
    for (Note child : children) {
      BareNote addedNote =
          graphRAGService.addNoteToRelatedNotes(
              relatedNotes, child, RelationshipToFocusNote.Child, budget);

      if (addedNote != null) {
        focus.getChildren().add(addedNote.getUriAndTitle());
        budget -= addedNote.getDetails().length() / GraphRAGService.CHARACTERS_PER_TOKEN;
      }
    }
    handleNext(focusNote, focus, relatedNotes, budget);
  }
}
