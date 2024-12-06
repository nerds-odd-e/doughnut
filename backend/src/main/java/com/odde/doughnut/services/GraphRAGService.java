package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;

public class GraphRAGService {
  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    // Create handlers with the focus note
    ParentRelationshipHandler parentHandler = new ParentRelationshipHandler(focusNote);
    ObjectRelationshipHandler objectHandler = new ObjectRelationshipHandler(focusNote);
    ContextualPathRelationshipHandler contextualPathHandler =
        new ContextualPathRelationshipHandler(focusNote);
    ChildRelationshipHandler childrenHandler = new ChildRelationshipHandler(focusNote);
    YoungerSiblingRelationshipHandler youngerSiblingHandler =
        new YoungerSiblingRelationshipHandler(focusNote);

    // Set up priority layers
    PriorityLayer priorityOneLayer =
        new PriorityLayer(parentHandler, objectHandler, contextualPathHandler);
    PriorityLayer priorityTwoLayer = new PriorityLayer(childrenHandler, youngerSiblingHandler);

    priorityOneLayer.setNextLayer(priorityTwoLayer);

    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(focusNote, tokenBudgetForRelatedNotes);
    priorityOneLayer.handle(builder);
    return builder.build();
  }
}
