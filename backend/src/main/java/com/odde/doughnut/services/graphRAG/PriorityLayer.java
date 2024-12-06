package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.ArrayList;
import java.util.List;

public class PriorityLayer {
  private final List<RelationshipHandler> handlers;
  private PriorityLayer nextLayer;

  public PriorityLayer(RelationshipHandler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("At least one handler is required");
    }
    this.handlers = new ArrayList<>(List.of(handlers));
  }

  public void setNextLayer(PriorityLayer nextLayer) {
    this.nextLayer = nextLayer;
  }

  public void handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    // Handle each handler in this layer
    for (RelationshipHandler handler : handlers) {
      handler.handle(focusNote, focus, relatedNotes);
    }

    // Move to next layer if exists
    if (nextLayer != null) {
      nextLayer.handle(focusNote, focus, relatedNotes);
    }
  }
}
