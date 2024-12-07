package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;

public class GraphRAGService {
  private final TokenCountingStrategy tokenCountingStrategy;

  public GraphRAGService(TokenCountingStrategy tokenCountingStrategy) {
    this.tokenCountingStrategy = tokenCountingStrategy;
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    // Create priority four layer first so we can pass it to ParentSiblingHandler
    PriorityLayer priorityFourLayer = new PriorityLayer(2);
    PriorityLayer priorityThreeLayer = new PriorityLayer(2);

    // Set up priority layers with number of notes to process before switching
    PriorityLayer priorityOneLayer =
        new PriorityLayer(
            3,
            new RelationshipHandler[] {
              new ParentRelationshipHandler(focusNote),
              new ObjectRelationshipHandler(focusNote),
              new AncestorInContextualPathRelationshipHandler(focusNote)
            });
    PriorityLayer priorityTwoLayer =
        new PriorityLayer(
            3,
            new RelationshipHandler[] {
              new ChildRelationshipHandler(focusNote, priorityThreeLayer, priorityFourLayer),
              new PriorSiblingRelationshipHandler(focusNote),
              new YoungerSiblingRelationshipHandler(focusNote),
              new InboundReferenceRelationshipHandler(
                  focusNote, priorityThreeLayer, priorityFourLayer),
              new AncestorInObjectContextualPathRelationshipHandler(focusNote),
              new SiblingOfParentRelationshipHandler(focusNote, priorityFourLayer),
              new SiblingOfParentOfObjectRelationshipHandler(focusNote, priorityFourLayer)
            });

    priorityOneLayer.setNextLayer(priorityTwoLayer);
    priorityTwoLayer.setNextLayer(priorityThreeLayer);
    priorityThreeLayer.setNextLayer(priorityFourLayer);

    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(focusNote, tokenBudgetForRelatedNotes, tokenCountingStrategy);
    priorityOneLayer.handle(builder);
    return builder.build();
  }
}
