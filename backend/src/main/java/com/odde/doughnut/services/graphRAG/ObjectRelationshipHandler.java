package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.List;

public class ObjectRelationshipHandler extends RelationshipHandler {
  private final GraphRAGService graphRAGService;

  public ObjectRelationshipHandler(GraphRAGService graphRAGService) {
    this.graphRAGService = graphRAGService;
  }

  @Override
  public void handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    if (focusNote.getTargetNote() != null) {
      graphRAGService.addNoteToRelatedNotes(
          relatedNotes, focusNote.getTargetNote(), RelationshipToFocusNote.Object);
    }
  }
}
