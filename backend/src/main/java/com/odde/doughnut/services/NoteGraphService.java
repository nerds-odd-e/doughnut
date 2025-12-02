package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.DepthQueryService;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;
import com.odde.doughnut.services.graphRAG.GraphRAGResultBuilder;
import com.odde.doughnut.services.graphRAG.TokenCountingStrategy;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;

public class NoteGraphService {
  private final TokenCountingStrategy tokenCountingStrategy;
  private final DepthQueryService depthQueryService;

  public NoteGraphService(TokenCountingStrategy tokenCountingStrategy) {
    this.tokenCountingStrategy = tokenCountingStrategy;
    this.depthQueryService = new DepthQueryService();
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(focusNote, tokenBudgetForRelatedNotes, tokenCountingStrategy);

    // Step 2.1: Fetch parent and object at depth 1
    var depth1Notes = depthQueryService.queryDepth1ParentAndObject(focusNote);
    for (Note note : depth1Notes) {
      RelationshipToFocusNote relationship;
      if (note.equals(focusNote.getParent())) {
        relationship = RelationshipToFocusNote.Parent;
      } else if (note.equals(focusNote.getTargetNote())) {
        relationship = RelationshipToFocusNote.Object;
      } else {
        continue; // Skip if not parent or object
      }
      builder.addNoteToRelatedNotes(note, relationship);
    }

    return builder.build();
  }
}
