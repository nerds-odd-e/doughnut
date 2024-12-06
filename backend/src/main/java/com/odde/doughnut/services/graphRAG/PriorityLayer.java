package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;

public class PriorityLayer {
  private final List<RelationshipHandler> handlers;
  @Setter private PriorityLayer nextLayer;

  public PriorityLayer(RelationshipHandler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("At least one handler is required");
    }
    this.handlers = new ArrayList<>(List.of(handlers));
  }

  public void handle(GraphRAGResultBuilder builder) {
    boolean anyHandlerActive;

    do {
      anyHandlerActive = false;
      for (RelationshipHandler handler : handlers) {
        Note relatedNote = handler.handle();

        if (relatedNote != null) {
          BareNote result =
              builder.addNoteToRelatedNotes(relatedNote, handler.getRelationshipToFocusNote());
          if (result != null) {
            handler.afterHandledSuccessfully(builder.getFocusNote(), result);
            anyHandlerActive = true;
          }
        }
      }
    } while (anyHandlerActive);

    if (nextLayer != null) {
      nextLayer.handle(builder);
    }
  }
}
