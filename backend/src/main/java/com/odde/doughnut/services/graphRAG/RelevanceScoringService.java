package com.odde.doughnut.services.graphRAG;

public class RelevanceScoringService {
  private static final double W_RELATIONSHIP = 100.0;
  private static final double RELATIONSHIP_TYPE_WEIGHT = 10.0;

  public double calculateScore(CandidateNote candidate) {
    // Step 2.5: Simple scoring with relationship type only
    // All depth 1 relationship types get equal weight for now
    return W_RELATIONSHIP * RELATIONSHIP_TYPE_WEIGHT;
  }
}
