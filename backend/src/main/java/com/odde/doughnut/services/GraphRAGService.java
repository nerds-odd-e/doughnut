package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final ObjectMapper objectMapper;

  @Autowired
  public GraphRAGService(
      TokenCountingStrategy tokenCountingStrategy,
      NoteRepository noteRepository,
      ObjectMapper objectMapper) {
    this.tokenCountingStrategy = tokenCountingStrategy;
    this.noteRepository = noteRepository;
    this.objectMapper = objectMapper;
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
              new TargetRelationshipHandler(focusNote),
              new AncestorInContextualPathRelationshipHandler(focusNote)
            });
    List<RelationshipHandler> priorityTwoHandlers = new ArrayList<>();
    priorityTwoHandlers.add(
        new ChildRelationshipHandler(focusNote, priorityThreeLayer, priorityFourLayer));
    priorityTwoHandlers.add(new PriorSiblingRelationshipHandler(focusNote));
    priorityTwoHandlers.add(new YoungerSiblingRelationshipHandler(focusNote));
    priorityTwoHandlers.add(
        new InboundReferenceRelationshipHandler(focusNote, priorityThreeLayer, priorityFourLayer));
    priorityTwoHandlers.add(new AncestorInTargetContextualPathRelationshipHandler(focusNote));
    priorityTwoHandlers.add(new SiblingOfParentRelationshipHandler(focusNote, priorityFourLayer));
    priorityTwoHandlers.add(
        new SiblingOfParentOfTargetRelationshipHandler(focusNote, priorityFourLayer));
    priorityTwoHandlers.add(
        new TargetSiblingRelationshipHandler(focusNote, noteRepository, priorityThreeLayer));
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

  public String getGraphRAGDescription(Note note) {
    GraphRAGResult retrieve = retrieve(note, 2500);
    String jsonString;
    try {
      jsonString = objectMapper.writeValueAsString(retrieve);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return """
          Focus Note and the notes related to it:
          %s
          """
        .formatted(jsonString);
  }
}
