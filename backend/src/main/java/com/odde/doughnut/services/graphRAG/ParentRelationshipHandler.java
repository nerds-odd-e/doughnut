package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.List;

public class ParentRelationshipHandler extends RelationshipHandler {
  private final GraphRAGService graphRAGService;
  private boolean exhausted = false;

  public ParentRelationshipHandler(GraphRAGService graphRAGService) {
    super(RelationshipToFocusNote.Parent);
    this.graphRAGService = graphRAGService;
  }

  @Override
  public Note handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    if (exhausted) {
      return null;
    }
    exhausted = true;
    return focusNote.getParent();
  }
}
