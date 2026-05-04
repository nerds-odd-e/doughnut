package com.odde.doughnut.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.graphRAG.*;
import com.odde.doughnut.services.graphRAG.relationships.*;
import java.util.ArrayList;
import java.util.List;
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

    List<Note> focusStructuralPeers = noteService.findStructuralPeerNotesInOrder(focusNote);
    List<RelationshipHandler> handlers = new ArrayList<>();
    List<Note> referencesForViewer =
        wikiTitleCacheService.referencesNotesForViewer(focusNote, viewer);
    List<Note> outgoingWikiLinkTargets =
        wikiTitleCacheService.outgoingWikiLinkTargetNotesForViewer(focusNote, viewer);
    if (!referencesForViewer.isEmpty()) {
      handlers.add(new ReferenceByRelationshipHandler(referencesForViewer));
    }
    if (!outgoingWikiLinkTargets.isEmpty()) {
      handlers.add(new OutgoingWikiLinkRelationshipHandler(outgoingWikiLinkTargets));
    }
    handlers.add(new OlderSiblingRelationshipHandler(focusNote, focusStructuralPeers));
    handlers.add(new YoungerSiblingRelationshipHandler(focusNote, focusStructuralPeers));

    PriorityLayer layer = new PriorityLayer(3, handlers.toArray(new RelationshipHandler[0]));

    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(
            focusNote,
            tokenBudgetForRelatedNotes,
            tokenCountingStrategy,
            wikiTitleCacheService,
            viewer);
    layer.handle(builder);
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
