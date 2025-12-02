package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.BareNote;
import com.odde.doughnut.services.graphRAG.CandidateNote;
import com.odde.doughnut.services.graphRAG.DepthQueryService;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;
import com.odde.doughnut.services.graphRAG.GraphRAGResultBuilder;
import com.odde.doughnut.services.graphRAG.InboundReferenceSelectionService;
import com.odde.doughnut.services.graphRAG.RelationshipTypeDerivationService;
import com.odde.doughnut.services.graphRAG.RelevanceScoringService;
import com.odde.doughnut.services.graphRAG.TokenCountingStrategy;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoteGraphService {
  private static final int CHILD_CAP_MULTIPLIER = 2;

  private final TokenCountingStrategy tokenCountingStrategy;
  private final DepthQueryService depthQueryService;
  private final RelationshipTypeDerivationService relationshipTypeDerivationService;
  private final RelevanceScoringService relevanceScoringService;
  private final InboundReferenceSelectionService inboundReferenceSelectionService;

  public NoteGraphService(TokenCountingStrategy tokenCountingStrategy) {
    this.tokenCountingStrategy = tokenCountingStrategy;
    this.depthQueryService = new DepthQueryService();
    this.relationshipTypeDerivationService = new RelationshipTypeDerivationService();
    this.relevanceScoringService = new RelevanceScoringService();
    this.inboundReferenceSelectionService = new InboundReferenceSelectionService();
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(focusNote, tokenBudgetForRelatedNotes, tokenCountingStrategy);

    // Step 3.1: Initialize tracking structures
    Map<Note, Integer> depthFetched = new HashMap<>();
    Map<Note, Integer> childrenEmitted = new HashMap<>();
    Map<Note, Integer> inboundEmitted = new HashMap<>();

    // Focus note is at depth 0
    depthFetched.put(focusNote, 0);

    // Step 2.5: Collect all candidates first
    List<CandidateNote> candidates = new ArrayList<>();

    // Step 2.1: Fetch parent and object at depth 1
    var depth1Notes = depthQueryService.queryDepth1ParentAndObject(focusNote);
    for (Note note : depth1Notes) {
      depthFetched.put(note, 1);
      RelationshipToFocusNote relationship =
          relationshipTypeDerivationService.deriveRelationshipType(note, focusNote);
      if (relationship != null) {
        candidates.add(new CandidateNote(note, relationship, 1));
      }
    }

    // Step 3.3: Fetch and apply per-depth caps for inbound references at depth 1
    var allInboundReferences = depthQueryService.queryDepth1InboundReferences(focusNote);
    int currentDepth = 1;
    int alreadyEmitted = inboundEmitted.getOrDefault(focusNote, 0);
    var selectedInboundReferences =
        inboundReferenceSelectionService.selectInboundReferences(
            focusNote, currentDepth, depthFetched, alreadyEmitted, allInboundReferences);
    for (Note note : selectedInboundReferences) {
      depthFetched.putIfAbsent(note, 1);
      RelationshipToFocusNote relationship =
          relationshipTypeDerivationService.deriveRelationshipType(note, focusNote);
      if (relationship != null) {
        candidates.add(new CandidateNote(note, relationship, 1));
      }
    }
    inboundEmitted.put(focusNote, alreadyEmitted + selectedInboundReferences.size());

    // Step 3.1: Fetch and apply per-depth caps for children at depth 1
    var allChildren = depthQueryService.queryDepth1Children(focusNote);
    int childCap = calculateChildCap(focusNote, currentDepth, depthFetched);
    int childrenAlreadyEmitted = childrenEmitted.getOrDefault(focusNote, 0);
    int childrenRemainingBudget = Math.max(0, childCap - childrenAlreadyEmitted);
    var selectedChildren =
        allChildren.subList(0, Math.min(childrenRemainingBudget, allChildren.size()));
    for (Note child : selectedChildren) {
      depthFetched.putIfAbsent(child, 1);
      RelationshipToFocusNote relationship =
          relationshipTypeDerivationService.deriveRelationshipType(child, focusNote);
      if (relationship != null) {
        candidates.add(new CandidateNote(child, relationship, 1));
      }
    }
    childrenEmitted.put(focusNote, childrenAlreadyEmitted + selectedChildren.size());

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
        // Step 3.5: Update focus note's relationship lists dynamically
        if (candidate.getRelationshipType() == RelationshipToFocusNote.Child) {
          builder.getFocusNote().getChildren().add(candidate.getNote().getUri());
        } else if (candidate.getRelationshipType() == RelationshipToFocusNote.InboundReference) {
          builder.getFocusNote().getInboundReferences().add(candidate.getNote().getUri());
        } else if (candidate.getRelationshipType() == RelationshipToFocusNote.PriorSibling) {
          builder.getFocusNote().getPriorSiblings().add(candidate.getNote().getUri());
        } else if (candidate.getRelationshipType() == RelationshipToFocusNote.YoungerSibling) {
          builder.getFocusNote().getYoungerSiblings().add(candidate.getNote().getUri());
        }
      }
    }

    return builder.build();
  }

  // Step 3.1: Calculate child cap for a parent note at a given depth
  private int calculateChildCap(Note parent, int currentDepth, Map<Note, Integer> depthFetched) {
    int parentDepthFetched = depthFetched.getOrDefault(parent, currentDepth);
    return CHILD_CAP_MULTIPLIER * (currentDepth - parentDepthFetched);
  }
}
