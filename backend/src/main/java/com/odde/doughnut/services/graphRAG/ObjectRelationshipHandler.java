package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.List;

public class ObjectRelationshipHandler extends RelationshipHandler {
  private final GraphRAGService graphRAGService;
  private boolean exhausted = false;

  public ObjectRelationshipHandler(GraphRAGService graphRAGService) {
    super(RelationshipToFocusNote.Object);
    this.graphRAGService = graphRAGService;
  }

  @Override
  public BareNote handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    if (exhausted) {
      return null;
    }
    exhausted = true;
    if (focusNote.getTargetNote() != null) {
      return graphRAGService.addNoteToRelatedNotes(
          relatedNotes, focusNote.getTargetNote(), getRelationshipToFocusNote());
    }
    return null;
  }
}
