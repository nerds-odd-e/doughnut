package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.graphRAG.*;
import com.odde.doughnut.services.graphRAG.relationships.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GraphRAGService {
  private final TokenCountingStrategy tokenCountingStrategy;
  private final NoteService noteService;
  private final ObjectMapper objectMapper;
  private final AuthorizationService authorizationService;
  private final WikiTitleCacheService wikiTitleCacheService;

  @Autowired
  public GraphRAGService(
      TokenCountingStrategy tokenCountingStrategy,
      NoteService noteService,
      ObjectMapper objectMapper,
      AuthorizationService authorizationService,
      WikiTitleCacheService wikiTitleCacheService) {
    this.tokenCountingStrategy = tokenCountingStrategy;
    this.noteService = noteService;
    this.objectMapper = objectMapper;
    this.authorizationService = authorizationService;
    this.wikiTitleCacheService = wikiTitleCacheService;
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    return retrieve(focusNote, tokenBudgetForRelatedNotes, authorizationService.getCurrentUser());
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes, User viewer) {
    // Create priority four layer first so we can pass it to ParentSiblingHandler
    PriorityLayer priorityFourLayer = new PriorityLayer(2);
    PriorityLayer priorityThreeLayer = new PriorityLayer(2);

    List<Note> focusStructuralPeers = noteService.findStructuralPeerNotesInOrder(focusNote);
    Note parent = focusNote.getParent();
    List<Note> parentStructuralPeers =
        parent != null ? noteService.findStructuralPeerNotesInOrder(parent) : List.of();
    Note primaryTarget =
        wikiTitleCacheService.primaryWikiLinkedTargetForGraph(focusNote, viewer).orElse(null);
    Note targetParent = primaryTarget != null ? primaryTarget.getParent() : null;
    List<Note> targetParentStructuralPeers =
        targetParent != null ? noteService.findStructuralPeerNotesInOrder(targetParent) : List.of();

    // Set up priority layers with number of notes to process before switching
    PriorityLayer priorityOneLayer =
        new PriorityLayer(
            3,
            new RelationshipHandler[] {
              new ParentRelationshipHandler(focusNote),
              new TargetRelationshipHandler(primaryTarget),
              new ContextAncestorRelationshipHandler(focusNote)
            });
    List<RelationshipHandler> priorityTwoHandlers = new ArrayList<>();
    priorityTwoHandlers.add(new ChildRelationshipHandler(focusNote));
    for (Note reference : wikiTitleCacheService.referencesNotesForViewer(focusNote, viewer)) {
      priorityTwoHandlers.add(
          new ReferenceByRelationshipHandler(
              List.of(reference), priorityThreeLayer, priorityFourLayer));
    }
    priorityTwoHandlers.add(new OlderSiblingRelationshipHandler(focusNote, focusStructuralPeers));
    priorityTwoHandlers.add(new YoungerSiblingRelationshipHandler(focusNote, focusStructuralPeers));
    priorityTwoHandlers.add(new TargetContextAncestorRelationshipHandler(primaryTarget));
    priorityTwoHandlers.add(
        new ParentSiblingRelationshipHandler(
            focusNote, priorityFourLayer, parent, parentStructuralPeers));
    priorityTwoHandlers.add(
        new TargetParentSiblingRelationshipHandler(
            focusNote, priorityFourLayer, targetParent, targetParentStructuralPeers));
    priorityTwoHandlers.add(
        new SiblingOfTargetRelationshipHandler(
            wikiTitleCacheService.siblingWikiLinkReferrersToPrimaryTargetForGraph(
                focusNote, viewer),
            priorityThreeLayer));
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
