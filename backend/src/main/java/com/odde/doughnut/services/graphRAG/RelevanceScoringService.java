package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Random;

public class RelevanceScoringService {
  private static final double W_RELATIONSHIP = 100.0;
  private static final double W_DEPTH = 20.0;
  private static final double W_RECENCY = 5.0;
  private static final double JITTER_RANGE = 0.5;

  // Relationship type weights
  private static final double CORE_CONTEXT_WEIGHT = 10.0;
  private static final double STRUCTURAL_CONTEXT_WEIGHT = 5.0;

  private static final Map<RelationshipToFocusNote, Double> RELATIONSHIP_WEIGHTS =
      Map.ofEntries(
          // Core context (highest)
          Map.entry(RelationshipToFocusNote.Parent, CORE_CONTEXT_WEIGHT),
          Map.entry(RelationshipToFocusNote.Child, CORE_CONTEXT_WEIGHT),
          Map.entry(RelationshipToFocusNote.Object, CORE_CONTEXT_WEIGHT),
          Map.entry(RelationshipToFocusNote.ObjectOfReifiedChild, CORE_CONTEXT_WEIGHT),
          Map.entry(RelationshipToFocusNote.InboundReference, CORE_CONTEXT_WEIGHT),
          Map.entry(RelationshipToFocusNote.SubjectOfInboundReference, CORE_CONTEXT_WEIGHT),
          // Structural context (medium)
          Map.entry(RelationshipToFocusNote.AncestorInContextualPath, STRUCTURAL_CONTEXT_WEIGHT),
          Map.entry(
              RelationshipToFocusNote.AncestorInObjectContextualPath, STRUCTURAL_CONTEXT_WEIGHT),
          Map.entry(RelationshipToFocusNote.PriorSibling, STRUCTURAL_CONTEXT_WEIGHT),
          Map.entry(RelationshipToFocusNote.YoungerSibling, STRUCTURAL_CONTEXT_WEIGHT),
          Map.entry(RelationshipToFocusNote.SiblingOfParent, STRUCTURAL_CONTEXT_WEIGHT),
          Map.entry(RelationshipToFocusNote.SiblingOfParentOfObject, STRUCTURAL_CONTEXT_WEIGHT),
          Map.entry(RelationshipToFocusNote.ChildOfSiblingOfParent, STRUCTURAL_CONTEXT_WEIGHT),
          Map.entry(
              RelationshipToFocusNote.ChildOfSiblingOfParentOfObject, STRUCTURAL_CONTEXT_WEIGHT),
          Map.entry(
              RelationshipToFocusNote.InboundReferenceContextualPath, STRUCTURAL_CONTEXT_WEIGHT),
          Map.entry(
              RelationshipToFocusNote.SiblingOfSubjectOfInboundReference,
              STRUCTURAL_CONTEXT_WEIGHT),
          // Soft/remote context (lowest) - Note: GrandChild and RemotelyRelated not yet in enum
          Map.entry(
              RelationshipToFocusNote.InboundReferenceToObjectOfReifiedChild,
              STRUCTURAL_CONTEXT_WEIGHT));

  private final Random random = new Random();

  public double calculateScore(CandidateNote candidate) {
    double relationshipWeight = getRelationshipWeight(candidate.getRelationshipType());
    double depthBonus = calculateDepthBonus(candidate.getDepthFetched());
    double recencyBonus = calculateRecencyBonus(candidate.getNote().getCreatedAt());
    double jitter = calculateJitter();

    return W_RELATIONSHIP * relationshipWeight
        + W_DEPTH * depthBonus
        + W_RECENCY * recencyBonus
        + jitter;
  }

  private double getRelationshipWeight(RelationshipToFocusNote relationshipType) {
    return RELATIONSHIP_WEIGHTS.getOrDefault(relationshipType, CORE_CONTEXT_WEIGHT);
  }

  private double calculateDepthBonus(int depth) {
    // depth 1 → 1.0, depth 2 → 0.7, depth 3 → 0.4
    return switch (depth) {
      case 1 -> 1.0;
      case 2 -> 0.7;
      case 3 -> 0.4;
      default -> 0.0;
    };
  }

  private double calculateRecencyBonus(Timestamp createdAt) {
    if (createdAt == null) {
      return 0.0;
    }
    long now = System.currentTimeMillis();
    long created = createdAt.getTime();
    double ageDays = (now - created) / (24.0 * 60.0 * 60.0 * 1000.0);
    // Exponential decay: exp(-age_days / 365.0)
    return Math.exp(-ageDays / 365.0);
  }

  private double calculateJitter() {
    // Random value between -JITTER_RANGE and +JITTER_RANGE
    return (random.nextDouble() * 2.0 - 1.0) * JITTER_RANGE;
  }
}
