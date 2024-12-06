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
      int tokens = graphRAGService.estimateTokens(child);
      if (tokens <= budget) {
        String childUriAndTitle = child.getUriAndTitle();
        focus.getChildren().add(childUriAndTitle);

        graphRAGService.addNoteToRelatedNotes(
            relatedNotes, child, RelationshipToFocusNote.Child, budget);
        budget -= tokens;
      }
    }
    handleNext(focusNote, focus, relatedNotes, budget);
  }
}
