package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.graphRAG.*;
import com.odde.doughnut.services.graphRAG.relationships.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GraphRAGService {
  private final TokenCountingStrategy tokenCountingStrategy;
  private final NoteRepository noteRepository;

  @Autowired
  public GraphRAGService(
      TokenCountingStrategy tokenCountingStrategy, NoteRepository noteRepository) {
    this.tokenCountingStrategy = tokenCountingStrategy;
    this.noteRepository = noteRepository;
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
    List<RelationshipHandler> priorityTwoHandlers = new ArrayList<>();
    priorityTwoHandlers.add(
        new ChildRelationshipHandler(focusNote, priorityThreeLayer, priorityFourLayer));
    priorityTwoHandlers.add(new PriorSiblingRelationshipHandler(focusNote));
    priorityTwoHandlers.add(new YoungerSiblingRelationshipHandler(focusNote));
    priorityTwoHandlers.add(
        new InboundReferenceRelationshipHandler(focusNote, priorityThreeLayer, priorityFourLayer));
    priorityTwoHandlers.add(new AncestorInObjectContextualPathRelationshipHandler(focusNote));
    priorityTwoHandlers.add(new SiblingOfParentRelationshipHandler(focusNote, priorityFourLayer));
    priorityTwoHandlers.add(
        new SiblingOfParentOfObjectRelationshipHandler(focusNote, priorityFourLayer));
    priorityTwoHandlers.add(new ObjectSiblingRelationshipHandler(focusNote, noteRepository));
    PriorityLayer priorityTwoLayer =
        new PriorityLayer(3, priorityTwoHandlers.toArray(new RelationshipHandler[0]));

    priorityOneLayer.setNextLayer(priorityTwoLayer);
    priorityTwoLayer.setNextLayer(priorityThreeLayer);
    priorityThreeLayer.setNextLayer(priorityFourLayer);

    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(focusNote, tokenBudgetForRelatedNotes, tokenCountingStrategy);
    priorityOneLayer.handle(builder);
    return builder.build();
  }
}
