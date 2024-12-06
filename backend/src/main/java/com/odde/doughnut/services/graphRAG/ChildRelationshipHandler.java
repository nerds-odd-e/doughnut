package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.List;

public class ChildRelationshipHandler extends RelationshipHandler {
  private final GraphRAGService graphRAGService;
  private int currentChildIndex = 0;
  private boolean exhausted = false;

  public ChildRelationshipHandler(GraphRAGService graphRAGService) {
    super(RelationshipToFocusNote.Child);
    this.graphRAGService = graphRAGService;
  }

  @Override
  public Note handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    if (exhausted) {
      return null;
    }
    List<Note> children = focusNote.getChildren();

    if (currentChildIndex < children.size()) {
      currentChildIndex++;
      return children.get(currentChildIndex);
    } else {
      exhausted = true;
    }
    return null;
  }

  @Override
  public void afterHandledSuccessfully(FocusNote focus, BareNote addedNote) {
    focus.getChildren().add(addedNote.getUriAndTitle());
  }
}
