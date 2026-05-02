package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.graphRAG.*;
import com.odde.doughnut.services.graphRAG.relationships.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GraphRAGService {
  private final TokenCountingStrategy tokenCountingStrategy;
  private final NoteService noteService;
  private final ObjectMapper objectMapper;
  private final AuthorizationService authorizationService;
  private final WikiTitleCacheService wikiTitleCacheService;
  private final NoteRepository noteRepository;

  @Autowired
  public GraphRAGService(
      TokenCountingStrategy tokenCountingStrategy,
      NoteService noteService,
      ObjectMapper objectMapper,
      AuthorizationService authorizationService,
      WikiTitleCacheService wikiTitleCacheService,
      NoteRepository noteRepository) {
    this.tokenCountingStrategy = tokenCountingStrategy;
    this.noteService = noteService;
    this.objectMapper = objectMapper;
    this.authorizationService = authorizationService;
    this.wikiTitleCacheService = wikiTitleCacheService;
    this.noteRepository = noteRepository;
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    return retrieve(focusNote, tokenBudgetForRelatedNotes, authorizationService.getCurrentUser());
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes, User viewer) {
    focusNote =
        Optional.ofNullable(focusNote.getId())
            .flatMap(
                id ->
                    noteRepository
                        .hydrateNonDeletedNotesWithNotebookAndFolderByIds(List.of(id))
                        .stream()
                        .findFirst())
            .orElse(focusNote);

    // Create priority four layer first so we can pass it to ParentSiblingHandler
    PriorityLayer priorityFourLayer = new PriorityLayer(2);
    PriorityLayer priorityThreeLayer = new PriorityLayer(2);

    List<Note> focusStructuralPeers =
        peersSharingTreeParent(focusNote, noteService.findStructuralPeerNotesInOrder(focusNote));
    Note parent = focusNote.getParent();
    List<Note> parentStructuralPeers =
        parent != null
            ? peersSharingTreeParent(parent, noteService.findStructuralPeerNotesInOrder(parent))
            : List.of();
    Note primaryTarget =
        wikiTitleCacheService.primaryWikiLinkedTargetForGraph(focusNote, viewer).orElse(null);
    Note targetParent = primaryTarget != null ? primaryTarget.getParent() : null;
    List<Note> targetParentStructuralPeers =
        targetParent != null
            ? peersSharingTreeParent(
                targetParent, noteService.findStructuralPeerNotesInOrder(targetParent))
            : List.of();

    // Set up priority layers with number of notes to process before switching
    PriorityLayer priorityOneLayer =
        new PriorityLayer(
            3,
            new RelationshipHandler[] {
              new TargetRelationshipHandler(primaryTarget),
            });
    List<RelationshipHandler> priorityTwoHandlers = new ArrayList<>();
    priorityTwoHandlers.add(new ChildRelationshipHandler(focusNote));
    List<Note> referencesForViewer =
        wikiTitleCacheService.referencesNotesForViewer(focusNote, viewer);
    if (!referencesForViewer.isEmpty()) {
      priorityTwoHandlers.add(
          new ReferenceByRelationshipHandler(
              referencesForViewer, priorityThreeLayer, priorityFourLayer));
    }
    priorityTwoHandlers.add(new OlderSiblingRelationshipHandler(focusNote, focusStructuralPeers));
    priorityTwoHandlers.add(new YoungerSiblingRelationshipHandler(focusNote, focusStructuralPeers));
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
        new GraphRAGResultBuilder(
            focusNote,
            tokenBudgetForRelatedNotes,
            tokenCountingStrategy,
            wikiTitleCacheService,
            viewer);
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

  /**
   * Folder scope lists every note in the folder; graph siblings are same tree-parent peers only.
   */
  private static List<Note> peersSharingTreeParent(Note note, List<Note> folderScopePeers) {
    Note treeParent = note.getParent();
    return folderScopePeers.stream()
        .filter(p -> Objects.equals(p.getParent(), treeParent))
        .toList();
  }
}
