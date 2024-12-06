package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.List;

public class ParentRelationshipHandler extends RelationshipHandler {
  private final GraphRAGService graphRAGService;

  public ParentRelationshipHandler(GraphRAGService graphRAGService) {
    this.graphRAGService = graphRAGService;
  }

  @Override
  public void handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    if (focusNote.getParent() != null) {
      String parentUriAndTitle = focusNote.getParent().getUriAndTitle();
      focus.getContextualPath().add(parentUriAndTitle);

      graphRAGService.addNoteToRelatedNotes(
          relatedNotes, focusNote.getParent(), RelationshipToFocusNote.Parent);
    }
    handleNext(focusNote, focus, relatedNotes);
  }
}
