package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.BareNote;
import com.odde.doughnut.services.graphRAG.CandidateNote;
import com.odde.doughnut.services.graphRAG.DepthQueryService;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;
import com.odde.doughnut.services.graphRAG.GraphRAGResultBuilder;
import com.odde.doughnut.services.graphRAG.RelationshipTypeDerivationService;
import com.odde.doughnut.services.graphRAG.RelevanceScoringService;
import com.odde.doughnut.services.graphRAG.TokenCountingStrategy;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NoteGraphService {
  private final TokenCountingStrategy tokenCountingStrategy;
  private final DepthQueryService depthQueryService;
  private final RelationshipTypeDerivationService relationshipTypeDerivationService;
  private final RelevanceScoringService relevanceScoringService;

  public NoteGraphService(TokenCountingStrategy tokenCountingStrategy) {
    this.tokenCountingStrategy = tokenCountingStrategy;
    this.depthQueryService = new DepthQueryService();
    this.relationshipTypeDerivationService = new RelationshipTypeDerivationService();
    this.relevanceScoringService = new RelevanceScoringService();
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(focusNote, tokenBudgetForRelatedNotes, tokenCountingStrategy);

    // Step 2.5: Collect all candidates first
    List<CandidateNote> candidates = new ArrayList<>();

    // Step 2.1: Fetch parent and object at depth 1
    var depth1Notes = depthQueryService.queryDepth1ParentAndObject(focusNote);
    for (Note note : depth1Notes) {
      RelationshipToFocusNote relationship =
          relationshipTypeDerivationService.deriveRelationshipType(note, focusNote);
      if (relationship != null) {
        candidates.add(new CandidateNote(note, relationship, 1));
      }
    }

    // Step 2.2: Fetch inbound references at depth 1
    var inboundReferences = depthQueryService.queryDepth1InboundReferences(focusNote);
    for (Note note : inboundReferences) {
      RelationshipToFocusNote relationship =
          relationshipTypeDerivationService.deriveRelationshipType(note, focusNote);
      if (relationship != null) {
        candidates.add(new CandidateNote(note, relationship, 1));
      }
    }

    // Step 2.3: Fetch children at depth 1
    var children = depthQueryService.queryDepth1Children(focusNote);
    for (Note child : children) {
      RelationshipToFocusNote relationship =
          relationshipTypeDerivationService.deriveRelationshipType(child, focusNote);
      if (relationship != null) {
        candidates.add(new CandidateNote(child, relationship, 1));
      }
    }

    // Step 2.5: Score all candidates
    for (CandidateNote candidate : candidates) {
      double score = relevanceScoringService.calculateScore(candidate);
      candidate.setRelevanceScore(score);
    }

    // Step 2.5: Sort by score (descending)
    candidates.sort(Comparator.comparing(CandidateNote::getRelevanceScore).reversed());

    // Step 2.5: Select top candidates that fit in budget
    for (CandidateNote candidate : candidates) {
      BareNote addedNote =
          builder.addNoteToRelatedNotes(candidate.getNote(), candidate.getRelationshipType());
      if (addedNote != null) {
        // Update focus note's relationship lists
        if (candidate.getRelationshipType() == RelationshipToFocusNote.Child) {
          builder.getFocusNote().getChildren().add(candidate.getNote().getUri());
        } else if (candidate.getRelationshipType() == RelationshipToFocusNote.InboundReference) {
          builder.getFocusNote().getInboundReferences().add(candidate.getNote().getUri());
        }
      }
    }

    return builder.build();
  }
}
