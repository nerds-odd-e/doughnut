package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.*;

public class DepthTraversalService {
  private final Map<Note, Integer> depthFetched = new HashMap<>();
  private final Map<Note, List<RelationshipToFocusNote>> discoveryPaths = new HashMap<>();
  private final Map<Note, Integer> childrenEmitted = new HashMap<>();
  private final Map<Note, Integer> inboundEmitted = new HashMap<>();
  private final ChildrenSelectionService childrenSelectionService;
  private final InboundReferenceSelectionService inboundSelectionService;
  private final RelevanceScoringService scoringService;
  private final TokenCountingStrategy tokenCountingStrategy;

  public DepthTraversalService(
      ChildrenSelectionService childrenSelectionService,
      InboundReferenceSelectionService inboundSelectionService,
      RelevanceScoringService scoringService,
      TokenCountingStrategy tokenCountingStrategy) {
    this.childrenSelectionService = childrenSelectionService;
    this.inboundSelectionService = inboundSelectionService;
    this.scoringService = scoringService;
    this.tokenCountingStrategy = tokenCountingStrategy;
  }

  public List<CandidateNote> traverseDepth(
      int depth, List<Note> sourceNotes, Note focusNote, int tokenBudget) {
    List<CandidateNote> candidates = new ArrayList<>();

    for (Note sourceNote : sourceNotes) {
      // Track depth for this source
      depthFetched.putIfAbsent(sourceNote, depth);

      // Select children
      int remainingBudget = estimateRemainingBudget(tokenBudget, candidates);
      List<Note> selectedChildren =
          childrenSelectionService.selectChildren(sourceNote, remainingBudget, depth, depthFetched);

      for (Note child : selectedChildren) {
        List<RelationshipToFocusNote> path = buildPath(sourceNote, RelationshipToFocusNote.Child);
        addCandidate(candidates, child, depth, path);
        updateEmittedCount(sourceNote, true);
      }

      // Select inbound references
      remainingBudget = estimateRemainingBudget(tokenBudget, candidates);
      List<Note> selectedInbound =
          inboundSelectionService.selectInboundReferences(
              sourceNote, remainingBudget, depth, depthFetched);

      for (Note inbound : selectedInbound) {
        List<RelationshipToFocusNote> path =
            buildPath(sourceNote, RelationshipToFocusNote.InboundReference);
        addCandidate(candidates, inbound, depth, path);
        updateEmittedCount(sourceNote, false);
      }
    }

    return candidates;
  }

  private List<RelationshipToFocusNote> buildPath(
      Note sourceNote, RelationshipToFocusNote relationship) {
    List<RelationshipToFocusNote> path = new ArrayList<>();
    List<RelationshipToFocusNote> sourcePath = discoveryPaths.getOrDefault(sourceNote, new ArrayList<>());
    path.addAll(sourcePath);
    path.add(relationship);
    return path;
  }

  private void addCandidate(
      List<CandidateNote> candidates,
      Note note,
      int depth,
      List<RelationshipToFocusNote> path) {
    // Skip if already discovered (use shortest path)
    if (discoveryPaths.containsKey(note)) {
      List<RelationshipToFocusNote> existingPath = discoveryPaths.get(note);
      if (path.size() < existingPath.size()) {
        // Update with shorter path
        discoveryPaths.put(note, path);
        depthFetched.put(note, depth);
      } else {
        // Keep existing shorter path
        return;
      }
    } else {
      discoveryPaths.put(note, path);
      depthFetched.put(note, depth);
    }

    // Create candidate with temporary score (will be recomputed later)
    CandidateNote candidate = new CandidateNote(note, depth, path, 0.0);
    candidates.add(candidate);
  }

  private void updateEmittedCount(Note note, boolean isChild) {
    if (isChild) {
      childrenEmitted.merge(note, 1, Integer::sum);
    } else {
      inboundEmitted.merge(note, 1, Integer::sum);
    }
  }

  private int estimateRemainingBudget(int totalBudget, List<CandidateNote> candidates) {
    int used = 0;
    for (CandidateNote candidate : candidates) {
      // Rough estimate: assume 1 token per candidate for now
      // This will be refined with actual token counting
      used += 1;
    }
    return Math.max(0, totalBudget - used);
  }

  public Map<Note, Integer> getDepthFetched() {
    return Collections.unmodifiableMap(depthFetched);
  }

  public Map<Note, List<RelationshipToFocusNote>> getDiscoveryPaths() {
    return Collections.unmodifiableMap(discoveryPaths);
  }
}
