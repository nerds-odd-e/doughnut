package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.BareNote;
import com.odde.doughnut.services.graphRAG.DepthQueryService;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;
import com.odde.doughnut.services.graphRAG.GraphRAGResultBuilder;
import com.odde.doughnut.services.graphRAG.RelationshipTypeDerivationService;
import com.odde.doughnut.services.graphRAG.TokenCountingStrategy;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;

public class NoteGraphService {
  private final TokenCountingStrategy tokenCountingStrategy;
  private final DepthQueryService depthQueryService;
  private final RelationshipTypeDerivationService relationshipTypeDerivationService;

  public NoteGraphService(TokenCountingStrategy tokenCountingStrategy) {
    this.tokenCountingStrategy = tokenCountingStrategy;
    this.depthQueryService = new DepthQueryService();
    this.relationshipTypeDerivationService = new RelationshipTypeDerivationService();
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(focusNote, tokenBudgetForRelatedNotes, tokenCountingStrategy);

    // Step 2.1: Fetch parent and object at depth 1
    var depth1Notes = depthQueryService.queryDepth1ParentAndObject(focusNote);
    for (Note note : depth1Notes) {
      RelationshipToFocusNote relationship =
          relationshipTypeDerivationService.deriveRelationshipType(note, focusNote);
      if (relationship != null) {
        builder.addNoteToRelatedNotes(note, relationship);
      }
    }

    // Step 2.2: Fetch inbound references at depth 1
    var inboundReferences = depthQueryService.queryDepth1InboundReferences(focusNote);
    for (Note note : inboundReferences) {
      RelationshipToFocusNote relationship =
          relationshipTypeDerivationService.deriveRelationshipType(note, focusNote);
      if (relationship != null) {
        BareNote addedNote = builder.addNoteToRelatedNotes(note, relationship);
        if (addedNote != null) {
          builder.getFocusNote().getInboundReferences().add(note.getUri());
        }
      }
    }

    // Step 2.3: Fetch children at depth 1
    var children = depthQueryService.queryDepth1Children(focusNote);
    for (Note child : children) {
      RelationshipToFocusNote relationship =
          relationshipTypeDerivationService.deriveRelationshipType(child, focusNote);
      if (relationship != null) {
        BareNote addedNote = builder.addNoteToRelatedNotes(child, relationship);
        if (addedNote != null) {
          builder.getFocusNote().getChildren().add(child.getUri());
        }
      }
    }

    return builder.build();
  }
}
