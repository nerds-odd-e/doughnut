package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.BareNote;
import com.odde.doughnut.services.graphRAG.CandidateNote;
import com.odde.doughnut.services.graphRAG.ChildrenSelectionService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NoteGraphService {
  private static final int CHILD_CAP_MULTIPLIER = 2;

  private final TokenCountingStrategy tokenCountingStrategy;
  private final DepthQueryService depthQueryService;
  private final RelationshipTypeDerivationService relationshipTypeDerivationService;
  private final RelevanceScoringService relevanceScoringService;
  private final InboundReferenceSelectionService inboundReferenceSelectionService;
  private final ChildrenSelectionService childrenSelectionService;

  public NoteGraphService(TokenCountingStrategy tokenCountingStrategy) {
    this.tokenCountingStrategy = tokenCountingStrategy;
    this.depthQueryService = new DepthQueryService();
    this.relationshipTypeDerivationService = new RelationshipTypeDerivationService();
    this.relevanceScoringService = new RelevanceScoringService();
    this.inboundReferenceSelectionService = new InboundReferenceSelectionService();
    this.childrenSelectionService = new ChildrenSelectionService();
  }

  public GraphRAGResult retrieve(Note focusNote, int tokenBudgetForRelatedNotes) {
    GraphRAGResultBuilder builder =
        new GraphRAGResultBuilder(focusNote, tokenBudgetForRelatedNotes, tokenCountingStrategy);

    // Step 3.1: Initialize tracking structures
    Map<Note, Integer> depthFetched = new HashMap<>();
    Map<Note, Integer> childrenEmitted = new HashMap<>();
    Map<Note, Integer> inboundEmitted = new HashMap<>();
    Map<Note, Set<Integer>> pickedChildIndices = new HashMap<>();

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

    // Step 3.2: Fetch and apply per-depth caps for children at depth 1 with ordered sibling
    // locality
    var allChildren = depthQueryService.queryDepth1Children(focusNote);
    int childCap = calculateChildCap(focusNote, currentDepth, depthFetched);
    int childrenAlreadyEmitted = childrenEmitted.getOrDefault(focusNote, 0);
    int childrenRemainingBudget = Math.max(0, childCap - childrenAlreadyEmitted);
    Set<Integer> pickedIndices =
        new HashSet<>(pickedChildIndices.getOrDefault(focusNote, new HashSet<>()));
    var selectedChildren =
        childrenSelectionService.selectChildren(
            focusNote, childrenRemainingBudget, pickedIndices, allChildren);
    // Update picked indices with newly selected children
    for (int i = 0; i < allChildren.size(); i++) {
      if (selectedChildren.contains(allChildren.get(i))) {
        pickedIndices.add(i);
      }
    }
    pickedChildIndices.put(focusNote, pickedIndices);
    for (Note child : selectedChildren) {
      depthFetched.putIfAbsent(child, 1);
      RelationshipToFocusNote relationship =
          relationshipTypeDerivationService.deriveRelationshipType(child, focusNote);
      if (relationship != null) {
        candidates.add(new CandidateNote(child, relationship, 1));
      }
    }
    childrenEmitted.put(focusNote, childrenAlreadyEmitted + selectedChildren.size());

    // Step 4.1: Depth 2 traversal - collect all depth 1 notes as source notes
    List<Note> depth1SourceNotes = new ArrayList<>();
    Map<Note, RelationshipToFocusNote> depth1NoteToRelationship = new HashMap<>();
    for (CandidateNote candidate : candidates) {
      if (candidate.getDepthFetched() == 1) {
        depth1SourceNotes.add(candidate.getNote());
        depth1NoteToRelationship.put(candidate.getNote(), candidate.getRelationshipType());
      }
    }

    // Step 4.1: Process depth 2 relationships from each depth 1 source note
    for (Note depth1Note : depth1SourceNotes) {
      RelationshipToFocusNote depth1Relationship = depth1NoteToRelationship.get(depth1Note);

      // Step 4.1: Process parent of depth 1 note
      // Step 4.3: Process parent chain recursively for contextual paths
      List<RelationshipToFocusNote> pathPrefix = new ArrayList<>();
      pathPrefix.add(depth1Relationship);
      processParentChainForContextualPath(
          depth1Note, pathPrefix, focusNote, depthFetched, candidates, 2);

      // Step 4.1: Process object of depth 1 note (if reification)
      if (depth1Note.getTargetNote() != null
          && !depthFetched.containsKey(depth1Note.getTargetNote())) {
        Note objectNote = depth1Note.getTargetNote();
        depthFetched.put(objectNote, 2);
        List<RelationshipToFocusNote> discoveryPath = new ArrayList<>();
        discoveryPath.add(depth1Relationship);
        discoveryPath.add(RelationshipToFocusNote.Object);
        RelationshipToFocusNote relationship =
            relationshipTypeDerivationService.deriveRelationshipType(
                objectNote, focusNote, discoveryPath);
        if (relationship != null) {
          candidates.add(new CandidateNote(objectNote, relationship, 2, discoveryPath));
          // Step 4.3: Process object's parent chain recursively for contextual paths
          List<RelationshipToFocusNote> objectPathPrefix = new ArrayList<>();
          objectPathPrefix.add(RelationshipToFocusNote.Object);
          processParentChainForContextualPath(
              objectNote, objectPathPrefix, focusNote, depthFetched, candidates, 2);
        }
      }

      // Step 4.1: Process children of depth 1 note with per-depth caps and selection logic
      List<Note> allDepth2Children = depthQueryService.queryDepth1Children(depth1Note);
      int depth2ChildCap = calculateChildCap(depth1Note, 2, depthFetched);
      int depth2ChildrenAlreadyEmitted = childrenEmitted.getOrDefault(depth1Note, 0);
      int depth2ChildrenRemainingBudget =
          Math.max(0, depth2ChildCap - depth2ChildrenAlreadyEmitted);
      // Step 4.2: If this is the focus note's parent, ensure we select enough children
      // to get all siblings after filtering out the focus note
      if (depth1Note.equals(focusNote.getParent()) && allDepth2Children.contains(focusNote)) {
        // Count how many siblings we need (all children except focus note)
        int numberOfSiblings = allDepth2Children.size() - 1;
        // We need to select at least (numberOfSiblings + 1) children to get all siblings
        // (numberOfSiblings siblings + focus note)
        int neededBudget = numberOfSiblings + 1;
        depth2ChildrenRemainingBudget =
            Math.max(
                depth2ChildrenRemainingBudget, Math.min(neededBudget, allDepth2Children.size()));
      }
      Set<Integer> depth2PickedIndices =
          new HashSet<>(pickedChildIndices.getOrDefault(depth1Note, new HashSet<>()));
      var selectedDepth2Children =
          childrenSelectionService.selectChildren(
              depth1Note, depth2ChildrenRemainingBudget, depth2PickedIndices, allDepth2Children);

      // Update picked indices (including focus note if it was selected, for tracking purposes)
      for (int i = 0; i < allDepth2Children.size(); i++) {
        if (selectedDepth2Children.contains(allDepth2Children.get(i))) {
          depth2PickedIndices.add(i);
        }
      }
      pickedChildIndices.put(depth1Note, depth2PickedIndices);

      // Step 4.2: Filter out focus note from selected children (it's already at depth 0)
      // This ensures siblings are properly processed without the focus note taking up a slot
      selectedDepth2Children =
          selectedDepth2Children.stream()
              .filter(child -> !child.equals(focusNote))
              .collect(java.util.stream.Collectors.toList());

      for (Note child : selectedDepth2Children) {
        if (!depthFetched.containsKey(child)) {
          depthFetched.put(child, 2);
          List<RelationshipToFocusNote> discoveryPath = new ArrayList<>();
          discoveryPath.add(depth1Relationship);
          discoveryPath.add(RelationshipToFocusNote.Child);
          RelationshipToFocusNote relationship =
              relationshipTypeDerivationService.deriveRelationshipType(
                  child, focusNote, discoveryPath);
          if (relationship != null) {
            candidates.add(new CandidateNote(child, relationship, 2, discoveryPath));
          }
        }
        // Step 4.4: Process object of reified child (if child is a reification)
        if (child.getTargetNote() != null && !depthFetched.containsKey(child.getTargetNote())) {
          Note objectNote = child.getTargetNote();
          depthFetched.put(objectNote, 2);
          List<RelationshipToFocusNote> objectDiscoveryPath = new ArrayList<>();
          objectDiscoveryPath.add(depth1Relationship);
          objectDiscoveryPath.add(RelationshipToFocusNote.Child);
          objectDiscoveryPath.add(RelationshipToFocusNote.Object);
          RelationshipToFocusNote objectRelationship =
              relationshipTypeDerivationService.deriveRelationshipType(
                  objectNote, focusNote, objectDiscoveryPath);
          if (objectRelationship != null) {
            candidates.add(
                new CandidateNote(objectNote, objectRelationship, 2, objectDiscoveryPath));
          }
        }
      }
      childrenEmitted.put(depth1Note, depth2ChildrenAlreadyEmitted + selectedDepth2Children.size());

      // Step 4.1: Process inbound references of depth 1 note with per-depth caps and selection
      // logic
      List<Note> allDepth2InboundRefs = depthQueryService.queryDepth1InboundReferences(depth1Note);
      int depth2InboundAlreadyEmitted = inboundEmitted.getOrDefault(depth1Note, 0);
      var selectedDepth2InboundRefs =
          inboundReferenceSelectionService.selectInboundReferences(
              depth1Note, 2, depthFetched, depth2InboundAlreadyEmitted, allDepth2InboundRefs);

      for (Note inboundRef : selectedDepth2InboundRefs) {
        if (!depthFetched.containsKey(inboundRef)) {
          depthFetched.put(inboundRef, 2);
          List<RelationshipToFocusNote> discoveryPath = new ArrayList<>();
          discoveryPath.add(depth1Relationship);
          discoveryPath.add(RelationshipToFocusNote.InboundReference);
          RelationshipToFocusNote relationship =
              relationshipTypeDerivationService.deriveRelationshipType(
                  inboundRef, focusNote, discoveryPath);
          if (relationship != null) {
            candidates.add(new CandidateNote(inboundRef, relationship, 2, discoveryPath));
          }
        }
      }
      inboundEmitted.put(
          depth1Note, depth2InboundAlreadyEmitted + selectedDepth2InboundRefs.size());
    }

    // Step 2.5: Score all candidates
    for (CandidateNote candidate : candidates) {
      double score = relevanceScoringService.calculateScore(candidate);
      candidate.setRelevanceScore(score);
    }

    // Step 2.5: Sort by score (descending)
    candidates.sort(Comparator.comparing(CandidateNote::getRelevanceScore).reversed());

    // Step 2.5: Select top candidates that fit in budget
    // Step 4.2: Collect siblings separately to sort by siblingOrder before adding to focus note
    List<CandidateNote> priorSiblings = new ArrayList<>();
    List<CandidateNote> youngerSiblings = new ArrayList<>();

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
          // Step 4.2: Collect prior siblings to sort by siblingOrder
          priorSiblings.add(candidate);
        } else if (candidate.getRelationshipType() == RelationshipToFocusNote.YoungerSibling) {
          // Step 4.2: Collect younger siblings to sort by siblingOrder
          youngerSiblings.add(candidate);
        }
      }
    }

    // Step 4.2: Sort siblings by siblingOrder and add to focus note's lists
    priorSiblings.sort(Comparator.comparing(c -> c.getNote().getSiblingOrder()));
    for (CandidateNote candidate : priorSiblings) {
      builder.getFocusNote().getPriorSiblings().add(candidate.getNote().getUri());
    }

    youngerSiblings.sort(Comparator.comparing(c -> c.getNote().getSiblingOrder()));
    for (CandidateNote candidate : youngerSiblings) {
      builder.getFocusNote().getYoungerSiblings().add(candidate.getNote().getUri());
    }

    return builder.build();
  }

  // Step 3.1: Calculate child cap for a parent note at a given depth
  private int calculateChildCap(Note parent, int currentDepth, Map<Note, Integer> depthFetched) {
    int parentDepthFetched = depthFetched.getOrDefault(parent, currentDepth);
    return CHILD_CAP_MULTIPLIER * (currentDepth - parentDepthFetched);
  }

  // Step 4.1: Calculate inbound cap for a target note at a given depth
  private int calculateInboundCap(Note target, int currentDepth, Map<Note, Integer> depthFetched) {
    int targetDepthFetched = depthFetched.getOrDefault(target, currentDepth);
    return 2 * (currentDepth - targetDepthFetched); // INBOUND_CAP_MULTIPLIER = 2
  }

  // Step 4.3: Process parent chain recursively for contextual paths
  private void processParentChainForContextualPath(
      Note currentNote,
      List<RelationshipToFocusNote> pathPrefix,
      Note focusNote,
      Map<Note, Integer> depthFetched,
      List<CandidateNote> candidates,
      int depth) {
    Note parentNote = currentNote.getParent();
    if (parentNote == null || depthFetched.containsKey(parentNote)) {
      return;
    }

    depthFetched.put(parentNote, depth);
    List<RelationshipToFocusNote> discoveryPath = new ArrayList<>(pathPrefix);
    discoveryPath.add(RelationshipToFocusNote.Parent);
    RelationshipToFocusNote relationship =
        relationshipTypeDerivationService.deriveRelationshipType(
            parentNote, focusNote, discoveryPath);
    if (relationship != null) {
      candidates.add(new CandidateNote(parentNote, relationship, depth, discoveryPath));
      // Continue processing parent chain if this is a contextual path ancestor
      if (relationship == RelationshipToFocusNote.AncestorInContextualPath
          || relationship == RelationshipToFocusNote.AncestorInObjectContextualPath) {
        processParentChainForContextualPath(
            parentNote, discoveryPath, focusNote, depthFetched, candidates, depth);
      }
    }
  }
}
