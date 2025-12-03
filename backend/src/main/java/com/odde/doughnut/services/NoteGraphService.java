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
import java.util.stream.Collectors;

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

    // Initialize tracking structures
    Map<Note, Integer> depthFetched = new HashMap<>();
    Map<Note, Integer> childrenEmitted = new HashMap<>();
    Map<Note, Integer> inboundEmitted = new HashMap<>();
    Map<Note, Set<Integer>> pickedChildIndices = new HashMap<>();

    depthFetched.put(focusNote, 0);
    List<CandidateNote> candidates = new ArrayList<>();

    // Process depth 1
    processDepth1(
        focusNote, depthFetched, childrenEmitted, inboundEmitted, pickedChildIndices, candidates);

    // Process depth 2
    processDepth2(
        focusNote, depthFetched, childrenEmitted, inboundEmitted, pickedChildIndices, candidates);

    // Score, sort, and select candidates
    scoreAndSelectCandidates(candidates, builder);

    return builder.build();
  }

  private void processDepth1(
      Note focusNote,
      Map<Note, Integer> depthFetched,
      Map<Note, Integer> childrenEmitted,
      Map<Note, Integer> inboundEmitted,
      Map<Note, Set<Integer>> pickedChildIndices,
      List<CandidateNote> candidates) {
    // Process parent and object
    var depth1Notes = depthQueryService.queryDepth1ParentAndObject(focusNote);
    for (Note note : depth1Notes) {
      depthFetched.put(note, 1);
      RelationshipToFocusNote relationship =
          relationshipTypeDerivationService.deriveRelationshipType(note, focusNote);
      if (relationship != null) {
        candidates.add(new CandidateNote(note, relationship, 1));
      }
    }

    // Process inbound references
    processInboundReferences(
        focusNote, focusNote, null, 1, depthFetched, inboundEmitted, candidates);

    // Process children
    processChildren(
        focusNote,
        focusNote,
        null,
        1,
        depthFetched,
        childrenEmitted,
        pickedChildIndices,
        candidates);
  }

  private void processDepth2(
      Note focusNote,
      Map<Note, Integer> depthFetched,
      Map<Note, Integer> childrenEmitted,
      Map<Note, Integer> inboundEmitted,
      Map<Note, Set<Integer>> pickedChildIndices,
      List<CandidateNote> candidates) {
    // Collect all depth 1 notes as source notes
    List<Note> depth1SourceNotes = new ArrayList<>();
    Map<Note, RelationshipToFocusNote> depth1NoteToRelationship = new HashMap<>();
    for (CandidateNote candidate : candidates) {
      if (candidate.getDepthFetched() == 1) {
        depth1SourceNotes.add(candidate.getNote());
        depth1NoteToRelationship.put(candidate.getNote(), candidate.getRelationshipType());
      }
    }

    // Process depth 2 relationships from each depth 1 source note
    for (Note depth1Note : depth1SourceNotes) {
      RelationshipToFocusNote depth1Relationship = depth1NoteToRelationship.get(depth1Note);

      // Process parent chain for contextual paths
      List<RelationshipToFocusNote> pathPrefix = new ArrayList<>();
      pathPrefix.add(depth1Relationship);
      processParentChainForContextualPath(
          depth1Note, pathPrefix, focusNote, depthFetched, candidates, 2);

      // Process object of depth 1 note (if reification)
      processObjectOfNote(depth1Note, depth1Relationship, focusNote, depthFetched, candidates, 2);

      // Process children
      processChildren(
          depth1Note,
          focusNote,
          depth1Relationship,
          2,
          depthFetched,
          childrenEmitted,
          pickedChildIndices,
          candidates);

      // Process inbound references
      processInboundReferences(
          depth1Note, focusNote, depth1Relationship, 2, depthFetched, inboundEmitted, candidates);
    }

    // Process subjects of inbound references discovered at depth 1
    // These should be discovered at depth 2 via the path [InboundReference, Subject]
    for (CandidateNote candidate : candidates) {
      if (candidate.getDepthFetched() == 1
          && candidate.getRelationshipType() == RelationshipToFocusNote.InboundReference) {
        processSubjectOfInboundReference(
            candidate.getNote(),
            candidate.getRelationshipType(),
            focusNote,
            depthFetched,
            candidates,
            2);
      }
    }
  }

  private void processChildren(
      Note parentNote,
      Note focusNote,
      RelationshipToFocusNote parentRelationship,
      int depth,
      Map<Note, Integer> depthFetched,
      Map<Note, Integer> childrenEmitted,
      Map<Note, Set<Integer>> pickedChildIndices,
      List<CandidateNote> candidates) {
    List<Note> allChildren = depthQueryService.queryDepth1Children(parentNote);
    int childCap = calculateChildCap(parentNote, depth, depthFetched);
    int childrenAlreadyEmitted = childrenEmitted.getOrDefault(parentNote, 0);
    int childrenRemainingBudget = Math.max(0, childCap - childrenAlreadyEmitted);

    // Special handling for focus note's parent to ensure all siblings are selected
    if (parentNote.equals(focusNote.getParent()) && allChildren.contains(focusNote)) {
      int numberOfSiblings = allChildren.size() - 1;
      int neededBudget = numberOfSiblings + 1;
      childrenRemainingBudget =
          Math.max(childrenRemainingBudget, Math.min(neededBudget, allChildren.size()));
    }

    Set<Integer> pickedIndices =
        new HashSet<>(pickedChildIndices.getOrDefault(parentNote, new HashSet<>()));
    var selectedChildren =
        childrenSelectionService.selectChildren(
            parentNote, childrenRemainingBudget, pickedIndices, allChildren);

    // Update picked indices
    for (int i = 0; i < allChildren.size(); i++) {
      if (selectedChildren.contains(allChildren.get(i))) {
        pickedIndices.add(i);
      }
    }
    pickedChildIndices.put(parentNote, pickedIndices);

    // Filter out focus note from selected children at depth 2
    if (depth == 2) {
      selectedChildren =
          selectedChildren.stream()
              .filter(child -> !child.equals(focusNote))
              .collect(Collectors.toList());
    }

    // Add children as candidates
    for (Note child : selectedChildren) {
      if (!depthFetched.containsKey(child)) {
        depthFetched.put(child, depth);
        RelationshipToFocusNote relationship;
        List<RelationshipToFocusNote> discoveryPath = null;
        if (depth == 1) {
          // Depth 1: use simple relationship derivation
          relationship = relationshipTypeDerivationService.deriveRelationshipType(child, focusNote);
        } else {
          // Depth 2+: use discovery path
          discoveryPath = createDiscoveryPath(parentRelationship, RelationshipToFocusNote.Child);
          relationship =
              relationshipTypeDerivationService.deriveRelationshipType(
                  child, focusNote, discoveryPath);
        }
        if (relationship != null) {
          if (discoveryPath != null) {
            candidates.add(new CandidateNote(child, relationship, depth, discoveryPath));
          } else {
            candidates.add(new CandidateNote(child, relationship, depth));
          }
        }
      }
      // Process object of reified child
      processObjectOfReifiedChild(
          child, parentRelationship, focusNote, depthFetched, candidates, depth);
    }

    childrenEmitted.put(parentNote, childrenAlreadyEmitted + selectedChildren.size());
  }

  private void processInboundReferences(
      Note targetNote,
      Note focusNote,
      RelationshipToFocusNote targetRelationship,
      int depth,
      Map<Note, Integer> depthFetched,
      Map<Note, Integer> inboundEmitted,
      List<CandidateNote> candidates) {
    List<Note> allInboundRefs = depthQueryService.queryDepth1InboundReferences(targetNote);
    int alreadyEmitted = inboundEmitted.getOrDefault(targetNote, 0);
    var selectedInboundRefs =
        inboundReferenceSelectionService.selectInboundReferences(
            targetNote, depth, depthFetched, alreadyEmitted, allInboundRefs);

    for (Note inboundRef : selectedInboundRefs) {
      if (!depthFetched.containsKey(inboundRef)) {
        depthFetched.put(inboundRef, depth);
        RelationshipToFocusNote relationship;
        List<RelationshipToFocusNote> discoveryPath = null;
        if (depth == 1) {
          // Depth 1: use simple relationship derivation
          relationship =
              relationshipTypeDerivationService.deriveRelationshipType(inboundRef, focusNote);
        } else {
          // Depth 2+: use discovery path
          discoveryPath =
              createDiscoveryPath(targetRelationship, RelationshipToFocusNote.InboundReference);
          relationship =
              relationshipTypeDerivationService.deriveRelationshipType(
                  inboundRef, focusNote, discoveryPath);
        }
        if (relationship != null) {
          if (discoveryPath != null) {
            candidates.add(new CandidateNote(inboundRef, relationship, depth, discoveryPath));
          } else {
            candidates.add(new CandidateNote(inboundRef, relationship, depth));
          }
        }
      }
      // Note: Subjects of inbound references are processed at depth 2, not depth 1
      // This ensures they are discovered via the path [InboundReference, Subject] at depth 2
    }

    inboundEmitted.put(targetNote, alreadyEmitted + selectedInboundRefs.size());
  }

  private void processObjectOfNote(
      Note note,
      RelationshipToFocusNote noteRelationship,
      Note focusNote,
      Map<Note, Integer> depthFetched,
      List<CandidateNote> candidates,
      int depth) {
    if (note.getTargetNote() == null || depthFetched.containsKey(note.getTargetNote())) {
      return;
    }

    Note objectNote = note.getTargetNote();
    depthFetched.put(objectNote, depth);
    List<RelationshipToFocusNote> discoveryPath =
        createDiscoveryPath(noteRelationship, RelationshipToFocusNote.Object);
    RelationshipToFocusNote relationship =
        relationshipTypeDerivationService.deriveRelationshipType(
            objectNote, focusNote, discoveryPath);
    if (relationship != null) {
      candidates.add(new CandidateNote(objectNote, relationship, depth, discoveryPath));
      // Process object's parent chain recursively for contextual paths
      List<RelationshipToFocusNote> objectPathPrefix = new ArrayList<>();
      objectPathPrefix.add(RelationshipToFocusNote.Object);
      processParentChainForContextualPath(
          objectNote, objectPathPrefix, focusNote, depthFetched, candidates, depth);
    }
  }

  private void processObjectOfReifiedChild(
      Note child,
      RelationshipToFocusNote parentRelationship,
      Note focusNote,
      Map<Note, Integer> depthFetched,
      List<CandidateNote> candidates,
      int depth) {
    if (child.getTargetNote() == null || depthFetched.containsKey(child.getTargetNote())) {
      return;
    }

    Note objectNote = child.getTargetNote();
    depthFetched.put(objectNote, depth);
    List<RelationshipToFocusNote> discoveryPath = new ArrayList<>();
    if (parentRelationship != null) {
      discoveryPath.add(parentRelationship);
    }
    discoveryPath.add(RelationshipToFocusNote.Child);
    discoveryPath.add(RelationshipToFocusNote.Object);
    RelationshipToFocusNote relationship =
        relationshipTypeDerivationService.deriveRelationshipType(
            objectNote, focusNote, discoveryPath);
    if (relationship != null) {
      candidates.add(new CandidateNote(objectNote, relationship, depth, discoveryPath));
    }
  }

  private void processSubjectOfInboundReference(
      Note inboundRef,
      RelationshipToFocusNote inboundRefRelationship,
      Note focusNote,
      Map<Note, Integer> depthFetched,
      List<CandidateNote> candidates,
      int depth) {
    if (inboundRef.getParent() == null || depthFetched.containsKey(inboundRef.getParent())) {
      return;
    }

    Note subjectNote = inboundRef.getParent();
    depthFetched.put(subjectNote, depth);
    List<RelationshipToFocusNote> discoveryPath = new ArrayList<>();
    // The path is always [InboundReference, Parent] for subjects discovered from depth 1
    // inbound references at depth 2
    discoveryPath.add(RelationshipToFocusNote.InboundReference);
    discoveryPath.add(RelationshipToFocusNote.Parent);
    RelationshipToFocusNote relationship =
        relationshipTypeDerivationService.deriveRelationshipType(
            subjectNote, focusNote, discoveryPath);
    if (relationship != null) {
      candidates.add(new CandidateNote(subjectNote, relationship, depth, discoveryPath));
    }
  }

  private List<RelationshipToFocusNote> createDiscoveryPath(
      RelationshipToFocusNote parentRelationship, RelationshipToFocusNote currentRelationship) {
    List<RelationshipToFocusNote> discoveryPath = new ArrayList<>();
    if (parentRelationship != null) {
      discoveryPath.add(parentRelationship);
    }
    discoveryPath.add(currentRelationship);
    return discoveryPath;
  }

  private void scoreAndSelectCandidates(
      List<CandidateNote> candidates, GraphRAGResultBuilder builder) {
    // Score all candidates
    for (CandidateNote candidate : candidates) {
      double score = relevanceScoringService.calculateScore(candidate);
      candidate.setRelevanceScore(score);
    }

    // Sort by score (descending)
    candidates.sort(Comparator.comparing(CandidateNote::getRelevanceScore).reversed());

    // Collect siblings separately to sort by siblingOrder before adding to focus note
    List<CandidateNote> priorSiblings = new ArrayList<>();
    List<CandidateNote> youngerSiblings = new ArrayList<>();

    // Select top candidates that fit in budget
    for (CandidateNote candidate : candidates) {
      BareNote addedNote =
          builder.addNoteToRelatedNotes(candidate.getNote(), candidate.getRelationshipType());
      if (addedNote != null) {
        updateFocusNoteRelationships(candidate, builder, priorSiblings, youngerSiblings);
      }
    }

    // Sort siblings by siblingOrder and add to focus note's lists
    priorSiblings.sort(Comparator.comparing(c -> c.getNote().getSiblingOrder()));
    for (CandidateNote candidate : priorSiblings) {
      builder.getFocusNote().getPriorSiblings().add(candidate.getNote().getUri());
    }

    youngerSiblings.sort(Comparator.comparing(c -> c.getNote().getSiblingOrder()));
    for (CandidateNote candidate : youngerSiblings) {
      builder.getFocusNote().getYoungerSiblings().add(candidate.getNote().getUri());
    }
  }

  private void updateFocusNoteRelationships(
      CandidateNote candidate,
      GraphRAGResultBuilder builder,
      List<CandidateNote> priorSiblings,
      List<CandidateNote> youngerSiblings) {
    RelationshipToFocusNote relationshipType = candidate.getRelationshipType();
    if (relationshipType == RelationshipToFocusNote.Child) {
      builder.getFocusNote().getChildren().add(candidate.getNote().getUri());
    } else if (relationshipType == RelationshipToFocusNote.InboundReference) {
      builder.getFocusNote().getInboundReferences().add(candidate.getNote().getUri());
    } else if (relationshipType == RelationshipToFocusNote.PriorSibling) {
      priorSiblings.add(candidate);
    } else if (relationshipType == RelationshipToFocusNote.YoungerSibling) {
      youngerSiblings.add(candidate);
    }
  }

  private int calculateChildCap(Note parent, int currentDepth, Map<Note, Integer> depthFetched) {
    int parentDepthFetched = depthFetched.getOrDefault(parent, currentDepth);
    return CHILD_CAP_MULTIPLIER * (currentDepth - parentDepthFetched);
  }

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
