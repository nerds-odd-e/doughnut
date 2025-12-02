package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.*;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.util.*;
import java.util.stream.Collectors;

public class GraphRAGService {
  private final TokenCountingStrategy tokenCountingStrategy;
  private final DepthQueryService depthQueryService;
  private final RelevanceScoringService scoringService;
  private final ChildrenSelectionService childrenSelectionService;
  private final InboundReferenceSelectionService inboundSelectionService;

  public GraphRAGService(TokenCountingStrategy tokenCountingStrategy) {
    this.tokenCountingStrategy = tokenCountingStrategy;
    this.depthQueryService = new DepthQueryService();
    this.scoringService = new RelevanceScoringService();
    this.childrenSelectionService = new ChildrenSelectionService();
    this.inboundSelectionService = new InboundReferenceSelectionService();
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    // Initialize candidate pool and tracking structures
    List<CandidateNote> candidatePool = new ArrayList<>();
    Map<Note, Integer> depthFetched = new HashMap<>();
    Map<Note, List<RelationshipToFocusNote>> discoveryPaths = new HashMap<>();
    Map<Note, CandidateNote> candidateMap = new HashMap<>();

    // Depth 1: Fetch direct relationships
    List<DepthQueryResult> depth1Results = depthQueryService.fetchDepth1Candidates(focusNote);
    List<Note> depth1SourceNotes = Collections.singletonList(focusNote);

    // Process depth 1 results and add special relationships
    processDepth1Results(
        focusNote, depth1Results, candidatePool, depthFetched, discoveryPaths, candidateMap);

    // BFS traversal for depths 2 to MAX_DEPTH
    List<Note> currentDepthSources = new ArrayList<>();
    for (CandidateNote candidate : candidatePool) {
      if (candidate.getDepthFetched() == 1) {
        currentDepthSources.add(candidate.getNote());
      }
    }

    for (int depth = 2; depth <= GraphRAGConstants.MAX_DEPTH; depth++) {
      if (currentDepthSources.isEmpty()
          || candidatePool.size() >= GraphRAGConstants.MAX_TOTAL_CANDIDATES) {
        break;
      }

      // Fetch candidates for this depth
      List<DepthQueryResult> depthResults =
          depthQueryService.fetchDepthNCandidates(currentDepthSources, depth);

      // Process depth N results
      List<Note> nextDepthSources =
          processDepthNResults(
              depth,
              depthResults,
              candidatePool,
              depthFetched,
              discoveryPaths,
              candidateMap,
              currentDepthSources);

      // Check token budget (hybrid approach)
      int estimatedTokens = estimateTotalTokens(candidatePool);
      if (estimatedTokens
          > tokenBudgetForRelatedNotes * GraphRAGConstants.TOKEN_BUDGET_SAFETY_MARGIN) {
        break;
      }

      currentDepthSources = nextDepthSources;
    }

    // Score all candidates
    for (CandidateNote candidate : candidatePool) {
      double score = scoringService.computeScore(candidate);
      // Update score in candidate (create new instance with updated score)
      candidateMap.put(
          candidate.getNote(),
          new CandidateNote(
              candidate.getNote(),
              candidate.getDepthFetched(),
              candidate.getDiscoveryPath(),
              score));
    }

    // Sort by relevance score descending
    List<CandidateNote> sortedCandidates =
        candidatePool.stream()
            .map(c -> candidateMap.get(c.getNote()))
            .sorted(Comparator.comparing(CandidateNote::getRelevanceScore).reversed())
            .collect(Collectors.toList());

    // Select top candidates that fit token budget
    // Sort by relationship priority first, then by score
    List<CandidateNote> prioritySortedCandidates =
        sortByPriorityThenScore(sortedCandidates);

    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(focusNote, tokenBudgetForRelatedNotes, tokenCountingStrategy);
    FocusNote focusNoteResult = builder.getFocusNote();

    // Populate FocusNote lists (children, siblings, inbound refs) as we add notes
    for (CandidateNote candidate : prioritySortedCandidates) {
      if (candidate.getNote().equals(focusNote)) {
        continue; // Skip focus note itself
      }
      BareNote addedNote =
          builder.addNoteToRelatedNotes(
              candidate.getNote(), candidate.getRelationshipType());
      if (addedNote == null) {
        // Token budget exhausted
        break;
      }

      // Update FocusNote lists based on what was actually added
      updateFocusNoteLists(candidate, focusNoteResult);
    }

    return builder.build();
  }

  private void processDepth1Results(
      Note focusNote,
      List<DepthQueryResult> results,
      List<CandidateNote> candidatePool,
      Map<Note, Integer> depthFetched,
      Map<Note, List<RelationshipToFocusNote>> discoveryPaths,
      Map<Note, CandidateNote> candidateMap) {
    // Process direct relationships
    for (DepthQueryResult result : results) {
      if (result.getNote().getDeletedAt() != null) {
        continue;
      }

      List<RelationshipToFocusNote> path =
          Collections.singletonList(result.getRelationshipType());
      depthFetched.put(result.getNote(), 1);
      discoveryPaths.put(result.getNote(), path);

      CandidateNote candidate = new CandidateNote(result.getNote(), 1, path, 0.0);
      candidatePool.add(candidate);
      candidateMap.put(result.getNote(), candidate);
    }

    // Add special relationships: PriorSibling, YoungerSibling
    addSiblingRelationships(focusNote, candidatePool, depthFetched, discoveryPaths, candidateMap);

    // Add AncestorInContextualPath (non-parent ancestors)
    addAncestorRelationships(focusNote, candidatePool, depthFetched, discoveryPaths, candidateMap);

    // Add AncestorInObjectContextualPath (if object exists)
    if (focusNote.getTargetNote() != null
        && focusNote.getTargetNote().getDeletedAt() == null) {
      addObjectAncestorRelationships(
          focusNote.getTargetNote(), candidatePool, depthFetched, discoveryPaths, candidateMap);
    }

    // Add SiblingOfParent relationships
    if (focusNote.getParent() != null && focusNote.getParent().getDeletedAt() == null) {
      addParentSiblingRelationships(
          focusNote.getParent(),
          focusNote,
          candidatePool,
          depthFetched,
          discoveryPaths,
          candidateMap);
    }

    // Add ObjectOfReifiedChild relationships
    addObjectOfReifiedChildRelationships(
        focusNote, candidatePool, depthFetched, discoveryPaths, candidateMap);

    // Add SubjectOfInboundReference relationships
    addSubjectOfInboundReferenceRelationships(
        focusNote, candidatePool, depthFetched, discoveryPaths, candidateMap);
  }

  private List<Note> processDepthNResults(
      int depth,
      List<DepthQueryResult> results,
      List<CandidateNote> candidatePool,
      Map<Note, Integer> depthFetched,
      Map<Note, List<RelationshipToFocusNote>> discoveryPaths,
      Map<Note, CandidateNote> candidateMap,
      List<Note> sourceNotes) {
    List<Note> nextDepthSources = new ArrayList<>();
    Map<Note, List<RelationshipToFocusNote>> sourcePaths = new HashMap<>();

    // Build source paths map
    for (Note source : sourceNotes) {
      sourcePaths.put(source, discoveryPaths.getOrDefault(source, new ArrayList<>()));
    }

    for (DepthQueryResult result : results) {
      if (result.getNote().getDeletedAt() != null) {
        continue;
      }

      Note sourceNote = result.getSourceNote();
      List<RelationshipToFocusNote> sourcePath = sourcePaths.getOrDefault(sourceNote, new ArrayList<>());
      List<RelationshipToFocusNote> path = new ArrayList<>(sourcePath);
      path.add(result.getRelationshipType());

      // Use shortest path
      if (discoveryPaths.containsKey(result.getNote())) {
        List<RelationshipToFocusNote> existingPath = discoveryPaths.get(result.getNote());
        if (path.size() < existingPath.size()) {
          discoveryPaths.put(result.getNote(), path);
          depthFetched.put(result.getNote(), depth);
        } else {
          continue; // Keep existing shorter path
        }
      } else {
        discoveryPaths.put(result.getNote(), path);
        depthFetched.put(result.getNote(), depth);
      }

      CandidateNote candidate = new CandidateNote(result.getNote(), depth, path, 0.0);
      candidatePool.add(candidate);
      candidateMap.put(result.getNote(), candidate);
      nextDepthSources.add(result.getNote());
    }

    return nextDepthSources;
  }

  private void addSiblingRelationships(
      Note focusNote,
      List<CandidateNote> candidatePool,
      Map<Note, Integer> depthFetched,
      Map<Note, List<RelationshipToFocusNote>> discoveryPaths,
      Map<Note, CandidateNote> candidateMap) {
    List<Note> siblings = focusNote.getSiblings();
    if (siblings.isEmpty()) {
      return;
    }

    int focusIndex = siblings.indexOf(focusNote);
    if (focusIndex < 0) {
      return;
    }

    // Prior siblings (before focus)
    for (int i = 0; i < focusIndex; i++) {
      Note sibling = siblings.get(i);
      if (sibling.getDeletedAt() != null) {
        continue;
      }
      List<RelationshipToFocusNote> path =
          Collections.singletonList(RelationshipToFocusNote.PriorSibling);
      addCandidateIfNew(
          sibling, 1, path, candidatePool, depthFetched, discoveryPaths, candidateMap);
    }

    // Younger siblings (after focus)
    for (int i = focusIndex + 1; i < siblings.size(); i++) {
      Note sibling = siblings.get(i);
      if (sibling.getDeletedAt() != null) {
        continue;
      }
      List<RelationshipToFocusNote> path =
          Collections.singletonList(RelationshipToFocusNote.YoungerSibling);
      addCandidateIfNew(
          sibling, 1, path, candidatePool, depthFetched, discoveryPaths, candidateMap);
    }
  }

  private void addAncestorRelationships(
      Note focusNote,
      List<CandidateNote> candidatePool,
      Map<Note, Integer> depthFetched,
      Map<Note, List<RelationshipToFocusNote>> discoveryPaths,
      Map<Note, CandidateNote> candidateMap) {
    List<Note> ancestors = focusNote.getAncestors();
    // Skip parent (already added as Parent)
    for (int i = 1; i < ancestors.size(); i++) {
      Note ancestor = ancestors.get(i);
      if (ancestor.getDeletedAt() != null) {
        continue;
      }
      List<RelationshipToFocusNote> path =
          Collections.singletonList(RelationshipToFocusNote.AncestorInContextualPath);
      addCandidateIfNew(
          ancestor, 1, path, candidatePool, depthFetched, discoveryPaths, candidateMap);
    }
  }

  private void addObjectAncestorRelationships(
      Note objectNote,
      List<CandidateNote> candidatePool,
      Map<Note, Integer> depthFetched,
      Map<Note, List<RelationshipToFocusNote>> discoveryPaths,
      Map<Note, CandidateNote> candidateMap) {
    List<Note> ancestors = objectNote.getAncestors();
    for (Note ancestor : ancestors) {
      if (ancestor.getDeletedAt() != null) {
        continue;
      }
      List<RelationshipToFocusNote> path =
          Collections.singletonList(RelationshipToFocusNote.AncestorInObjectContextualPath);
      addCandidateIfNew(
          ancestor, 1, path, candidatePool, depthFetched, discoveryPaths, candidateMap);
    }
  }

  private void addParentSiblingRelationships(
      Note parent,
      Note focusNote,
      List<CandidateNote> candidatePool,
      Map<Note, Integer> depthFetched,
      Map<Note, List<RelationshipToFocusNote>> discoveryPaths,
      Map<Note, CandidateNote> candidateMap) {
    List<Note> parentSiblings = parent.getSiblings();
    for (Note parentSibling : parentSiblings) {
      if (parentSibling.equals(parent) || parentSibling.getDeletedAt() != null) {
        continue;
      }
      List<RelationshipToFocusNote> path =
          Collections.singletonList(RelationshipToFocusNote.SiblingOfParent);
      addCandidateIfNew(
          parentSibling, 1, path, candidatePool, depthFetched, discoveryPaths, candidateMap);
    }
  }

  private void addObjectOfReifiedChildRelationships(
      Note focusNote,
      List<CandidateNote> candidatePool,
      Map<Note, Integer> depthFetched,
      Map<Note, List<RelationshipToFocusNote>> discoveryPaths,
      Map<Note, CandidateNote> candidateMap) {
    for (Note child : focusNote.getChildren()) {
      if (child.getDeletedAt() != null || child.getTargetNote() == null) {
        continue;
      }
      Note objectNote = child.getTargetNote();
      if (objectNote.getDeletedAt() != null) {
        continue;
      }
      List<RelationshipToFocusNote> path =
          Collections.singletonList(RelationshipToFocusNote.ObjectOfReifiedChild);
      addCandidateIfNew(
          objectNote, 1, path, candidatePool, depthFetched, discoveryPaths, candidateMap);
    }
  }

  private void addSubjectOfInboundReferenceRelationships(
      Note focusNote,
      List<CandidateNote> candidatePool,
      Map<Note, Integer> depthFetched,
      Map<Note, List<RelationshipToFocusNote>> discoveryPaths,
      Map<Note, CandidateNote> candidateMap) {
    for (Note inbound : focusNote.getInboundReferences()) {
      if (inbound.getDeletedAt() != null || inbound.getParent() == null) {
        continue;
      }
      Note subject = inbound.getParent();
      if (subject.getDeletedAt() != null) {
        continue;
      }
      List<RelationshipToFocusNote> path =
          Collections.singletonList(RelationshipToFocusNote.SubjectOfInboundReference);
      addCandidateIfNew(
          subject, 1, path, candidatePool, depthFetched, discoveryPaths, candidateMap);
    }
  }

  private void addCandidateIfNew(
      Note note,
      int depth,
      List<RelationshipToFocusNote> path,
      List<CandidateNote> candidatePool,
      Map<Note, Integer> depthFetched,
      Map<Note, List<RelationshipToFocusNote>> discoveryPaths,
      Map<Note, CandidateNote> candidateMap) {
    // Use shortest path
    if (discoveryPaths.containsKey(note)) {
      List<RelationshipToFocusNote> existingPath = discoveryPaths.get(note);
      if (path.size() < existingPath.size()) {
        discoveryPaths.put(note, path);
        depthFetched.put(note, depth);
        // Update candidate
        CandidateNote newCandidate = new CandidateNote(note, depth, path, 0.0);
        candidateMap.put(note, newCandidate);
        // Remove old candidate and add new one
        candidatePool.removeIf(c -> c.getNote().equals(note));
        candidatePool.add(newCandidate);
      }
    } else {
      discoveryPaths.put(note, path);
      depthFetched.put(note, depth);
      CandidateNote candidate = new CandidateNote(note, depth, path, 0.0);
      candidatePool.add(candidate);
      candidateMap.put(note, candidate);
    }
  }

  private int estimateTotalTokens(List<CandidateNote> candidates) {
    int total = 0;
    for (CandidateNote candidate : candidates) {
      // Rough estimate using token counting strategy
      BareNote bareNote = BareNote.fromNote(candidate.getNote(), candidate.getRelationshipType());
      total += tokenCountingStrategy.estimateTokens(bareNote);
    }
    return total;
  }

  private void updateFocusNoteLists(CandidateNote candidate, FocusNote focusNoteResult) {
    RelationshipToFocusNote relationship = candidate.getRelationshipType();
    String uri = candidate.getNote().getUri();

    switch (relationship) {
      case Child:
        if (!focusNoteResult.getChildren().contains(uri)) {
          focusNoteResult.getChildren().add(uri);
        }
        break;
      case PriorSibling:
        if (!focusNoteResult.getPriorSiblings().contains(uri)) {
          focusNoteResult.getPriorSiblings().add(uri);
        }
        break;
      case YoungerSibling:
        if (!focusNoteResult.getYoungerSiblings().contains(uri)) {
          focusNoteResult.getYoungerSiblings().add(uri);
        }
        break;
      case InboundReference:
        if (!focusNoteResult.getInboundReferences().contains(uri)) {
          focusNoteResult.getInboundReferences().add(uri);
        }
        break;
      default:
        // Other relationships don't go into FocusNote lists
        break;
    }
  }

  private List<CandidateNote> sortByPriorityThenScore(List<CandidateNote> candidates) {
    // Define relationship priority (lower number = higher priority)
    Map<RelationshipToFocusNote, Integer> priorityMap = new HashMap<>();
    priorityMap.put(RelationshipToFocusNote.Parent, 1);
    priorityMap.put(RelationshipToFocusNote.Object, 2);
    priorityMap.put(RelationshipToFocusNote.AncestorInContextualPath, 3);
    priorityMap.put(RelationshipToFocusNote.Child, 4);
    priorityMap.put(RelationshipToFocusNote.PriorSibling, 5);
    priorityMap.put(RelationshipToFocusNote.YoungerSibling, 6);
    priorityMap.put(RelationshipToFocusNote.InboundReference, 7);
    priorityMap.put(RelationshipToFocusNote.SubjectOfInboundReference, 8);
    priorityMap.put(RelationshipToFocusNote.AncestorInObjectContextualPath, 9);
    priorityMap.put(RelationshipToFocusNote.ObjectOfReifiedChild, 10);
    priorityMap.put(RelationshipToFocusNote.SiblingOfParent, 11);
    priorityMap.put(RelationshipToFocusNote.SiblingOfParentOfObject, 12);
    priorityMap.put(RelationshipToFocusNote.ChildOfSiblingOfParent, 13);
    priorityMap.put(RelationshipToFocusNote.ChildOfSiblingOfParentOfObject, 14);
    priorityMap.put(RelationshipToFocusNote.InboundReferenceContextualPath, 15);
    priorityMap.put(RelationshipToFocusNote.SiblingOfSubjectOfInboundReference, 16);
    priorityMap.put(RelationshipToFocusNote.InboundReferenceToObjectOfReifiedChild, 17);
    priorityMap.put(RelationshipToFocusNote.GrandChild, 18);
    priorityMap.put(RelationshipToFocusNote.RemotelyRelated, 99);

    return candidates.stream()
        .sorted(
            Comparator.comparing(
                    (CandidateNote c) ->
                        priorityMap.getOrDefault(c.getRelationshipType(), 99))
                .thenComparing(
                    (CandidateNote c) -> {
                      // For children, preserve siblingOrder (lower = earlier)
                      if (c.getRelationshipType() == RelationshipToFocusNote.Child) {
                        return c.getNote().getSiblingOrder() != null
                            ? (double) c.getNote().getSiblingOrder()
                            : Double.MAX_VALUE;
                      }
                      // For other relationships, use score (negative for descending)
                      return -c.getRelevanceScore();
                    }))
        .collect(Collectors.toList());
  }
}
