package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import java.util.List;

public class PriorityLayer {
  private final RelationshipHandler firstHandler;
  private PriorityLayer nextLayer;

  public PriorityLayer(RelationshipHandler... handlers) {
    if (handlers.length == 0) {
      throw new IllegalArgumentException("At least one handler is required");
    }

    this.firstHandler = handlers[0];
    RelationshipHandler current = firstHandler;

    // Chain the handlers within this layer
    for (int i = 1; i < handlers.length; i++) {
      current.setNext(handlers[i]);
      current = handlers[i];
    }
  }

  public void setNextLayer(PriorityLayer nextLayer) {
    this.nextLayer = nextLayer;
    // The last handler in this layer should move to the first handler of next layer
    getLastHandler().setNext(nextLayer.firstHandler);
  }

  private RelationshipHandler getLastHandler() {
    RelationshipHandler current = firstHandler;
    RelationshipHandler nextLayerFirstHandler = nextLayer != null ? nextLayer.firstHandler : null;
    while (current.next != null && current.next != nextLayerFirstHandler) {
      current = current.next;
    }
    return current;
  }

  public void handle(Note focusNote, FocusNote focus, List<BareNote> relatedNotes) {
    firstHandler.handle(focusNote, focus, relatedNotes);
  }
}
