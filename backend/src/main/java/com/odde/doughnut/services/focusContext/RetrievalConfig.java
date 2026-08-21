package com.odde.doughnut.services.focusContext;

import java.util.Optional;

public class RetrievalConfig {
  private static final int DEFAULT_MAX_DEPTH = 2;

  private final int maxDepth;
  private final Long sampleSeed;
  private final Integer contentTokenBudgetOverride;

  public RetrievalConfig(int maxDepth, Long sampleSeed, Integer contentTokenBudgetOverride) {
    this.maxDepth = maxDepth;
    this.sampleSeed = sampleSeed;
    this.contentTokenBudgetOverride = contentTokenBudgetOverride;
  }

  /** Default max traversal depth for question generation and similar flows. */
  public static RetrievalConfig defaultMaxDepth() {
    return new RetrievalConfig(DEFAULT_MAX_DEPTH, null, null);
  }

  public static RetrievalConfig forQuestionGeneration(Long sampleSeed) {
    return new RetrievalConfig(DEFAULT_MAX_DEPTH, sampleSeed, null);
  }

  public static RetrievalConfig forQuestionGeneration(Long sampleSeed, int contentTokenBudget) {
    return new RetrievalConfig(DEFAULT_MAX_DEPTH, sampleSeed, contentTokenBudget);
  }

  public static RetrievalConfig depth1() {
    return new RetrievalConfig(1, null, null);
  }

  /**
   * Combined approximate token budget for focus and related note content for {@code GET
   * /notes/{id}/graph}.
   */
  public static RetrievalConfig forGraphApi(int combinedContentTokenBudget) {
    return new RetrievalConfig(DEFAULT_MAX_DEPTH, null, combinedContentTokenBudget);
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  /**
   * When present, focus-context sampling (inbound wiki references and folder siblings) uses this
   * seed for deterministic SQL ordering (CRC32-based), not an in-memory shuffle of full candidate
   * lists.
   */
  public Optional<Long> getSampleSeed() {
    return Optional.ofNullable(sampleSeed);
  }

  /**
   * Approximate token budget for focus note content plus all related note content combined (bodies
   * only; same unit as {@link com.odde.doughnut.services.ApproximateUtf8TokenBudget}).
   */
  public int getFocusContextContentTokenBudget() {
    return contentTokenBudgetOverride != null
        ? contentTokenBudgetOverride
        : FocusContextConstants.FOCUS_CONTEXT_COMBINED_CONTENT_TOKEN_BUDGET;
  }
}
