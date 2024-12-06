package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;

public class GraphRAGService {
  private final PriorityLayer firstPriorityLayer;

  public GraphRAGService() {
    // Create handlers
    ParentRelationshipHandler parentHandler = new ParentRelationshipHandler();
    ObjectRelationshipHandler objectHandler = new ObjectRelationshipHandler();
    ChildRelationshipHandler childrenHandler = new ChildRelationshipHandler();
    YoungerSiblingRelationshipHandler youngerSiblingHandler =
        new YoungerSiblingRelationshipHandler();

    // Set up priority layers
    PriorityLayer priorityOneLayer = new PriorityLayer(parentHandler, objectHandler);
    PriorityLayer priorityTwoLayer = new PriorityLayer(childrenHandler, youngerSiblingHandler);

    priorityOneLayer.setNextLayer(priorityTwoLayer);
    this.firstPriorityLayer = priorityOneLayer;
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(focusNote, tokenBudgetForRelatedNotes);
    firstPriorityLayer.handle(focusNote, builder);
    return builder.build();
  }
}
