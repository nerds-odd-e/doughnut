package com.odde.doughnut.services.graphRAG;

import static com.odde.doughnut.services.graphRAG.GraphRAGConstants.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.*;
import java.util.stream.Collectors;

public class DepthTraversalService {
  private final DepthQueryService depthQueryService;
  private final ChildrenSelectionService childrenSelectionService;
  private final InboundReferenceSelectionService inboundReferenceSelectionService;
  private final TokenCountingStrategy tokenCountingStrategy;

  private final Map<Note, Integer> depthFetched = new HashMap<>();
  private final Map<Note, List<RelationshipToFocusNote>> discoveryPaths = new HashMap<>();
  private final Map<Note, Integer> childrenEmitted = new HashMap<>();
  private final Map<Note, Integer> inboundEmitted = new HashMap<>();
  private final Map<Note, CandidateNote> candidateMap = new HashMap<>();

  public DepthTraversalService(
      DepthQueryService depthQueryService,
      ChildrenSelectionService childrenSelectionService,
      InboundReferenceSelectionService inboundReferenceSelectionService,
      TokenCountingStrategy tokenCountingStrategy) {
    this.depthQueryService = depthQueryService;
    this.childrenSelectionService = childrenSelectionService;
    this.inboundReferenceSelectionService = inboundReferenceSelectionService;
    this.tokenCountingStrategy = tokenCountingStrategy;
  }

  public List<CandidateNote> traverseDepth(
      int depth, List<Note> sourceNotes, Note focusNote, int tokenBudget) {
    if (sourceNotes.isEmpty() || depth > MAX_DEPTH) {
      return new ArrayList<>();
    }

    List<DepthQueryResult> queryResults = depth == 1
        ? depthQueryService.fetchDepth1Candidates(focusNote)
        : depthQueryService.fetchDepthNCandidates(sourceNotes, depth);

    List<CandidateNote> newCandidates = new ArrayList<>();

    Map<Note, List<DepthQueryResult>> byParent = new HashMap<>();
    Map<Note, List<DepthQueryResult>> byTarget = new HashMap<>();

    for (DepthQueryResult result : queryResults) {
      if (result.getRelationshipType() == RelationshipToFocusNote.Child) {
        byParent.computeIfAbsent(result.getSourceNote(), k -> new ArrayList<>()).add(result);
      } else if (result.getRelationshipType() == RelationshipToFocusNote.InboundReference) {
        byTarget.computeIfAbsent(result.getSourceNote(), k -> new ArrayList<>()).add(result);
      } else {
        addCandidateIfNew(result, depth, focusNote, newCandidates);
      }
    }

    for (Map.Entry<Note, List<DepthQueryResult>> entry : byParent.entrySet()) {
      Note parent = entry.getKey();
      int remainingChildBudget = computeRemainingChildBudget(parent, depth);
      if (remainingChildBudget > 0) {
        List<Note> selectedChildren =
            childrenSelectionService.selectChildren(
                parent, remainingChildBudget, depth, depthFetched);
        for (Note child : selectedChildren) {
          List<RelationshipToFocusNote> path = buildPath(parent, RelationshipToFocusNote.Child);
          addCandidateIfNew(child, path, depth, focusNote, newCandidates);
          childrenEmitted.put(parent, childrenEmitted.getOrDefault(parent, 0) + 1);
        }
      }
    }

    for (Map.Entry<Note, List<DepthQueryResult>> entry : byTarget.entrySet()) {
      Note target = entry.getKey();
      int remainingInboundBudget = computeRemainingInboundBudget(target, depth);
      if (remainingInboundBudget > 0) {
        List<Note> selectedInbound =
            inboundReferenceSelectionService.selectInboundReferences(
                target, remainingInboundBudget, depth, depthFetched);
        for (Note inboundRef : selectedInbound) {
          List<RelationshipToFocusNote> path =
              buildPath(target, RelationshipToFocusNote.InboundReference);
          addCandidateIfNew(inboundRef, path, depth, focusNote, newCandidates);
          inboundEmitted.put(target, inboundEmitted.getOrDefault(target, 0) + 1);
        }
      }
    }

    return newCandidates;
  }

  private int computeRemainingChildBudget(Note parent, int depth) {
    int parentDepth = depthFetched.getOrDefault(parent, depth);
    int childCap = CHILD_CAP_MULTIPLIER * (depth - parentDepth);
    int emitted = childrenEmitted.getOrDefault(parent, 0);
    return Math.max(0, childCap - emitted);
  }

  private int computeRemainingInboundBudget(Note target, int depth) {
    int targetDepth = depthFetched.getOrDefault(target, depth);
    int inboundCap = INBOUND_CAP_MULTIPLIER * (depth - targetDepth);
    int emitted = inboundEmitted.getOrDefault(target, 0);
    return Math.max(0, inboundCap - emitted);
  }

  private void addCandidateIfNew(
      DepthQueryResult result, int depth, Note focusNote, List<CandidateNote> newCandidates) {
    List<RelationshipToFocusNote> path =
        buildPath(result.getSourceNote(), result.getRelationshipType());
    addCandidateIfNew(result.getNote(), path, depth, focusNote, newCandidates);
  }

  private void addCandidateIfNew(
      Note note,
      List<RelationshipToFocusNote> path,
      int depth,
      Note focusNote,
      List<CandidateNote> newCandidates) {
    if (note == focusNote || note.getDeletedAt() != null) {
      return;
    }

    CandidateNote existing = candidateMap.get(note);
    if (existing != null) {
      existing.updateRelationshipTypeIfShorterPath(path);
      return;
    }

    depthFetched.put(note, depth);
    discoveryPaths.put(note, new ArrayList<>(path));
    CandidateNote candidate = new CandidateNote(note, depth, path);
    candidateMap.put(note, candidate);
    newCandidates.add(candidate);
  }

  private List<RelationshipToFocusNote> buildPath(
      Note sourceNote, RelationshipToFocusNote relationship) {
    List<RelationshipToFocusNote> path = discoveryPaths.getOrDefault(sourceNote, new ArrayList<>());
    List<RelationshipToFocusNote> newPath = new ArrayList<>(path);
    newPath.add(relationship);
    return newPath;
  }

  public List<CandidateNote> getAllCandidates() {
    return new ArrayList<>(candidateMap.values());
  }

  public void initializeFocusNote(Note focusNote) {
    depthFetched.put(focusNote, 0);
    discoveryPaths.put(focusNote, new ArrayList<>());
  }
}

