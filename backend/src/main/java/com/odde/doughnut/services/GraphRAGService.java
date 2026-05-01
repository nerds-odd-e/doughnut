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
  private final NoteService noteService;
  private final ObjectMapper objectMapper;

  @Autowired
  public GraphRAGService(
      TokenCountingStrategy tokenCountingStrategy,
      NoteRepository noteRepository,
      NoteService noteService,
      ObjectMapper objectMapper) {
    this.tokenCountingStrategy = tokenCountingStrategy;
    this.noteRepository = noteRepository;
    this.noteService = noteService;
    this.objectMapper = objectMapper;
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    // Create priority four layer first so we can pass it to ParentSiblingHandler
    PriorityLayer priorityFourLayer = new PriorityLayer(2);
    PriorityLayer priorityThreeLayer = new PriorityLayer(2);

    List<Note> focusStructuralPeers = noteService.findStructuralPeerNotesInOrder(focusNote);
    Note parent = focusNote.getParent();
    List<Note> parentStructuralPeers =
        parent != null ? noteService.findStructuralPeerNotesInOrder(parent) : List.of();
    Note targetParent =
        focusNote.getTargetNote() != null ? focusNote.getTargetNote().getParent() : null;
    List<Note> targetParentStructuralPeers =
        targetParent != null ? noteService.findStructuralPeerNotesInOrder(targetParent) : List.of();

    // Set up priority layers with number of notes to process before switching
    PriorityLayer priorityOneLayer =
        new PriorityLayer(
            3,
            new RelationshipHandler[] {
              new ParentRelationshipHandler(focusNote),
              new TargetRelationshipHandler(focusNote),
              new ContextAncestorRelationshipHandler(focusNote)
            });
    List<RelationshipHandler> priorityTwoHandlers = new ArrayList<>();
    priorityTwoHandlers.add(
        new ChildRelationshipHandler(focusNote, priorityThreeLayer, priorityFourLayer));
    priorityTwoHandlers.add(new OlderSiblingRelationshipHandler(focusNote, focusStructuralPeers));
    priorityTwoHandlers.add(new YoungerSiblingRelationshipHandler(focusNote, focusStructuralPeers));
    priorityTwoHandlers.add(
        new ReferenceByRelationshipHandler(focusNote, priorityThreeLayer, priorityFourLayer));
    priorityTwoHandlers.add(new TargetContextAncestorRelationshipHandler(focusNote));
    priorityTwoHandlers.add(
        new ParentSiblingRelationshipHandler(
            focusNote, priorityFourLayer, parent, parentStructuralPeers));
    priorityTwoHandlers.add(
        new TargetParentSiblingRelationshipHandler(
            focusNote, priorityFourLayer, targetParent, targetParentStructuralPeers));
    priorityTwoHandlers.add(
        new SiblingOfTargetRelationshipHandler(focusNote, noteRepository, priorityThreeLayer));
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
