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
    NoteInObjectContextualPathRelationshipHandler objectContextualPathHandler =
        new NoteInObjectContextualPathRelationshipHandler(focusNote);

    // Create priority three layer first so we can pass it to ChildRelationshipHandler
    PriorityLayer priorityThreeLayer = new PriorityLayer(2);

    ChildRelationshipHandler childrenHandler =
        new ChildRelationshipHandler(focusNote, priorityThreeLayer);
    PriorSiblingRelationshipHandler priorSiblingHandler =
        new PriorSiblingRelationshipHandler(focusNote);
    YoungerSiblingRelationshipHandler youngerSiblingHandler =
        new YoungerSiblingRelationshipHandler(focusNote);
    ReferringNoteRelationshipHandler referringNoteHandler =
        new ReferringNoteRelationshipHandler(focusNote, priorityThreeLayer);

    // Set up priority layers with number of notes to process before switching
    PriorityLayer priorityOneLayer =
        new PriorityLayer(
            3, // Process 3 notes before switching to layer 2
            new RelationshipHandler[] {parentHandler, objectHandler, contextualPathHandler});
    PriorityLayer priorityTwoLayer =
        new PriorityLayer(
            3, // Process 3 notes before switching to layer 3
            new RelationshipHandler[] {
              childrenHandler,
              priorSiblingHandler,
              youngerSiblingHandler,
              referringNoteHandler,
              objectContextualPathHandler
            });

    priorityOneLayer.setNextLayer(priorityTwoLayer);
    priorityTwoLayer.setNextLayer(priorityThreeLayer);

    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(focusNote, tokenBudgetForRelatedNotes, tokenCountingStrategy);
    priorityOneLayer.handle(builder);
    return builder.build();
  }
}
