package com.odde.doughnut.services.focusContext;

import java.util.Optional;

public class RetrievalConfig {
  private static final int DEFAULT_MAX_DEPTH = 2;

  private final int maxDepth;
  private final Long sampleSeed;
  private final Integer relatedNotesTotalBudgetTokens;

  public RetrievalConfig(int maxDepth, Long sampleSeed, Integer relatedNotesBudget) {
    this.maxDepth = maxDepth;
    this.sampleSeed = sampleSeed;
    this.relatedNotesTotalBudgetTokens = relatedNotesBudget;
  }

  /** Default max traversal depth for question generation and similar flows. */
  public static RetrievalConfig defaultMaxDepth() {
    return new RetrievalConfig(DEFAULT_MAX_DEPTH, null, null);
  }

  public static RetrievalConfig forQuestionGeneration(Long sampleSeed) {
    return new RetrievalConfig(DEFAULT_MAX_DEPTH, sampleSeed, null);
  }

  public static RetrievalConfig depth1() {
    return new RetrievalConfig(1, null, null);
  }

  /** Related-note token budget for {@code GET /notes/{id}/graph}. */
  public static RetrievalConfig forGraphApi(int relatedNotesTokenBudget) {
    return new RetrievalConfig(DEFAULT_MAX_DEPTH, null, relatedNotesTokenBudget);
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  /**
   * When present, inbound references and folder sibling candidates are shuffled with {@link
   * java.util.Random} this seed.
   */
  public Optional<Long> getSampleSeed() {
    return Optional.ofNullable(sampleSeed);
  }

  public int getRelatedNotesTotalBudgetTokens() {
    return relatedNotesTotalBudgetTokens != null
        ? relatedNotesTotalBudgetTokens
        : FocusContextConstants.RELATED_NOTES_TOTAL_BUDGET_TOKENS;
  }
}
