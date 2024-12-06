package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;

public class PriorityLayer {
  private final List<RelationshipHandler> handlers;
  @Setter private PriorityLayer nextLayer;
  private int notesProcessedSinceLastLayerSwitch = 0;
  private static final int NOTES_BEFORE_NEXT_LAYER = 3;

  public PriorityLayer(RelationshipHandler... handlers) {
    this.handlers = new ArrayList<>();
    if (handlers != null && handlers.length > 0) {
      this.handlers.addAll(List.of(handlers));
    }
  }

  public void handle(GraphRAGResultBuilder builder) {
    boolean continueProcessing = true;

    while (continueProcessing) {
      continueProcessing = processCurrentLayer(builder);

      // If we've processed enough notes or can't process more in current layer,
      // give next layer a chance if it exists
      if (nextLayer != null
          && (notesProcessedSinceLastLayerSwitch >= NOTES_BEFORE_NEXT_LAYER
              || !continueProcessing)) {
        // Use handle instead of processCurrentLayer for proper recursion
        nextLayer.handle(builder);

        // Reset counter after giving next layer a chance
        notesProcessedSinceLastLayerSwitch = 0;

        // Try current layer again only if we stopped due to NOTES_BEFORE_NEXT_LAYER limit
        if (notesProcessedSinceLastLayerSwitch >= NOTES_BEFORE_NEXT_LAYER) {
          continueProcessing = true;
        }
      }

      // If we can't process more and there's no next layer, we're done
      if (!continueProcessing && nextLayer == null) {
        break;
      }
    }
  }

  private boolean processCurrentLayer(GraphRAGResultBuilder builder) {
    boolean anyHandlerProcessed = false;

    for (RelationshipHandler handler : handlers) {
      Note relatedNote = handler.handle();

      if (relatedNote != null) {
        BareNote result =
            builder.addNoteToRelatedNotes(relatedNote, handler.getRelationshipToFocusNote());

        if (result != null) {
          handler.afterHandledSuccessfully(builder.getFocusNote(), result);
          notesProcessedSinceLastLayerSwitch++;
          anyHandlerProcessed = true;

          // If we've hit the limit, give next layer a chance
          if (notesProcessedSinceLastLayerSwitch >= NOTES_BEFORE_NEXT_LAYER) {
            break;
          }
        }
      }
    }

    return anyHandlerProcessed;
  }

  public void addHandler(RelationshipHandler handler) {
    handlers.add(handler);
  }
}
