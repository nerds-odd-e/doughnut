package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class RelevanceScoringService {
  private static final double W_RELATIONSHIP = 100.0;
  private static final double W_DEPTH = 20.0;
  private static final double W_RECENCY = 5.0;

  private final Random random = new Random();

  public double computeScore(CandidateNote candidate) {
    double relationshipScore = getRelationshipWeight(candidate.getRelationshipType());
    double depthScore = getDepthBonus(candidate.getDepthFetched());
    double recencyScore = getRecencyBonus(candidate.getNote());

    // Add jitter (±0.5)
    double jitter = (random.nextDouble() - 0.5) * 1.0;

    return W_RELATIONSHIP * relationshipScore
        + W_DEPTH * depthScore
        + W_RECENCY * recencyScore
        + jitter;
  }

  private double getRelationshipWeight(RelationshipToFocusNote relationship) {
    // Core relationships: 10.0
    if (relationship == RelationshipToFocusNote.Parent
        || relationship == RelationshipToFocusNote.Object
        || relationship == RelationshipToFocusNote.Child) {
      return 10.0;
    }

    // Structural relationships: 5.0
    if (relationship == RelationshipToFocusNote.PriorSibling
        || relationship == RelationshipToFocusNote.YoungerSibling
        || relationship == RelationshipToFocusNote.InboundReference
        || relationship == RelationshipToFocusNote.AncestorInContextualPath
        || relationship == RelationshipToFocusNote.AncestorInObjectContextualPath
        || relationship == RelationshipToFocusNote.SiblingOfParent
        || relationship == RelationshipToFocusNote.SiblingOfParentOfObject
        || relationship == RelationshipToFocusNote.GrandChild) {
      return 5.0;
    }

    // Soft relationships: 2.0 (everything else)
    return 2.0;
  }

  private double getDepthBonus(int depth) {
    switch (depth) {
      case 1:
        return 1.0;
      case 2:
        return 0.7;
      case 3:
        return 0.4;
      default:
        return 0.1;
    }
  }

  private double getRecencyBonus(Note note) {
    if (note.getUpdatedAt() == null) {
      return 0.0;
    }

    Instant now = Instant.now();
    Instant updatedAt = note.getUpdatedAt().toInstant();
    long ageDays = Duration.between(updatedAt, now).toDays();

    // Exponential decay: exp(-age_days / 365.0)
    return Math.exp(-ageDays / 365.0);
  }
}
