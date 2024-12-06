package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GraphRAGService;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;

public class PriorityLayer {
  private final List<RelationshipHandler> handlers;
  private final GraphRAGService graphRAGService;
  @Setter private PriorityLayer nextLayer;

  public PriorityLayer(GraphRAGService graphRAGService, RelationshipHandler... handlers) {
    this.graphRAGService = graphRAGService;
    if (handlers.length == 0) {
      throw new IllegalArgumentException("At least one handler is required");
    }
    this.handlers = new ArrayList<>(List.of(handlers));
  }

  public void handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    // Handle each handler in this layer
    for (RelationshipHandler handler : handlers) {
      BareNote result;
      do {
        Note relatedNote = handler.handle(focusNote, focus, relatedNotes);

        result =
            graphRAGService.addNoteToRelatedNotes(
                relatedNotes, relatedNote, handler.getRelationshipToFocusNote());
        if (result != null) {
          handler.afterHandledSuccessfully(focus, result);
        }
      } while (result != null);
    }

    // Move to next layer if exists
    if (nextLayer != null) {
      nextLayer.handle(focusNote, focus, relatedNotes);
    }
  }
}
