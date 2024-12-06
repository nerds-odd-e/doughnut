package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;

public class GraphRAGService {
  private final TokenCountingStrategy tokenCountingStrategy;

  public GraphRAGService(TokenCountingStrategy tokenCountingStrategy) {
    this.tokenCountingStrategy = tokenCountingStrategy;
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    // Create handlers with the focus note
    ParentRelationshipHandler parentHandler = new ParentRelationshipHandler(focusNote);
    ObjectRelationshipHandler objectHandler = new ObjectRelationshipHandler(focusNote);
    ContextualPathRelationshipHandler contextualPathHandler =
        new ContextualPathRelationshipHandler(focusNote);

    // Create priority three layer first so we can pass it to ChildRelationshipHandler
    PriorityLayer priorityThreeLayer = new PriorityLayer();

    ChildRelationshipHandler childrenHandler =
        new ChildRelationshipHandler(focusNote, priorityThreeLayer);
    PriorSiblingRelationshipHandler priorSiblingHandler =
        new PriorSiblingRelationshipHandler(focusNote);
    YoungerSiblingRelationshipHandler youngerSiblingHandler =
        new YoungerSiblingRelationshipHandler(focusNote);
    ReferringNoteRelationshipHandler referringNoteHandler =
        new ReferringNoteRelationshipHandler(focusNote);

    // Set up priority layers
    PriorityLayer priorityOneLayer =
        new PriorityLayer(parentHandler, objectHandler, contextualPathHandler);
    PriorityLayer priorityTwoLayer =
        new PriorityLayer(
            childrenHandler, priorSiblingHandler, youngerSiblingHandler, referringNoteHandler);

    priorityOneLayer.setNextLayer(priorityTwoLayer);
    priorityTwoLayer.setNextLayer(priorityThreeLayer);

    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(focusNote, tokenBudgetForRelatedNotes, tokenCountingStrategy);
    priorityOneLayer.handle(builder);
    return builder.build();
  }
}
