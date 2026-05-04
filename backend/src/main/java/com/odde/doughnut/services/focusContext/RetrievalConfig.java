package com.odde.doughnut.services.focusContext;

import java.util.Optional;

public class RetrievalConfig {
  private static final int DEFAULT_MAX_DEPTH = 2;

  private final int maxDepth;
  private final Long folderSiblingSampleSeed;

  public RetrievalConfig(int maxDepth, Long folderSiblingSampleSeed) {
    this.maxDepth = maxDepth;
    this.folderSiblingSampleSeed = folderSiblingSampleSeed;
  }

  /** Default max traversal depth for question generation and similar flows. */
  public static RetrievalConfig defaultMaxDepth() {
    return new RetrievalConfig(DEFAULT_MAX_DEPTH, null);
  }

  public static RetrievalConfig forQuestionGeneration(Long folderSiblingSampleSeed) {
    return new RetrievalConfig(DEFAULT_MAX_DEPTH, folderSiblingSampleSeed);
  }

  public static RetrievalConfig depth1() {
    return new RetrievalConfig(1, null);
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  /**
   * When present, folder sibling candidates are shuffled with {@link java.util.Random} this seed.
   */
  public Optional<Long> getFolderSiblingSampleSeed() {
    return Optional.ofNullable(folderSiblingSampleSeed);
  }
}
