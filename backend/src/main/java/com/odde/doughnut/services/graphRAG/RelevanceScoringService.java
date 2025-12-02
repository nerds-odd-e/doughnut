package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.services.graphRAG.relationships.RelationshipToFocusNote;
import java.sql.Timestamp;
import java.util.Random;

public class RelevanceScoringService {
  private static final double W_RELATIONSHIP = 100.0;
  private static final double W_DEPTH = 20.0;
  private static final double W_RECENCY = 5.0;
  private static final double JITTER_RANGE = 0.5;
  private static final long MILLIS_PER_DAY = 24L * 60 * 60 * 1000;
  private static final double RECENCY_DECAY_DAYS = 365.0;

  private final Random random;

  public RelevanceScoringService() {
    this.random = new Random();
  }

  public RelevanceScoringService(Random random) {
    this.random = random;
  }

  public double computeScore(CandidateNote candidate) {
    double relationshipWeight = getRelationshipWeight(candidate.getRelationshipType());
    double depthBonus = getDepthBonus(candidate.getDepthFetched());
    double recencyBonus = getRecencyBonus(candidate.getNote().getCreatedAt());
    double jitter = getJitter();

    return W_RELATIONSHIP * relationshipWeight
        + W_DEPTH * depthBonus
        + W_RECENCY * recencyBonus
        + jitter;
  }

  private double getRelationshipWeight(RelationshipToFocusNote relationship) {
    switch (relationship) {
      case Self:
      case Parent:
      case Child:
      case Object:
      case ObjectOfReifiedChild:
      case InboundReference:
      case SubjectOfInboundReference:
        return 10.0;
      case AncestorInContextualPath:
      case AncestorInObjectContextualPath:
      case PriorSibling:
      case YoungerSibling:
      case SiblingOfParent:
      case SiblingOfParentOfObject:
      case ChildOfSiblingOfParent:
      case ChildOfSiblingOfParentOfObject:
      case InboundReferenceContextualPath:
      case SiblingOfSubjectOfInboundReference:
        return 5.0;
      case GrandChild:
      case RemotelyRelated:
      default:
        return 2.0;
    }
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

  private double getRecencyBonus(Timestamp createdAt) {
    if (createdAt == null) {
      return 0.5;
    }
    long now = System.currentTimeMillis();
    long created = createdAt.getTime();
    long ageMillis = now - created;
    double ageDays = (double) ageMillis / MILLIS_PER_DAY;
    return Math.exp(-ageDays / RECENCY_DECAY_DAYS);
  }

  private double getJitter() {
    return (random.nextDouble() - 0.5) * 2 * JITTER_RANGE;
  }
}

