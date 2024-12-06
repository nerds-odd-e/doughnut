package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.List;

public class ParentRelationshipHandler extends RelationshipHandler {
  private final GraphRAGService graphRAGService;
  private boolean exhausted = false;

  public ParentRelationshipHandler(GraphRAGService graphRAGService) {
    this.graphRAGService = graphRAGService;
  }

  @Override
  public BareNote handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    if (exhausted) {
      return null;
    }
    exhausted = true;
    if (focusNote.getParent() != null) {
      String parentUriAndTitle = focusNote.getParent().getUriAndTitle();
      focus.getContextualPath().add(parentUriAndTitle);

      return graphRAGService.addNoteToRelatedNotes(
          relatedNotes, focusNote.getParent(), RelationshipToFocusNote.Parent);
    }
    return null;
  }
}
