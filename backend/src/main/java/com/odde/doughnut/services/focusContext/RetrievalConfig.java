package com.odde.doughnut.services.focusContext;

public class RetrievalConfig {
  private final int maxDepth;

  public RetrievalConfig(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  /** Default max traversal depth for question generation and similar flows. */
  public static RetrievalConfig defaultMaxDepth() {
    return new RetrievalConfig(2);
  }

  public static RetrievalConfig depth1() {
    return new RetrievalConfig(1);
  }

  public int getMaxDepth() {
    return maxDepth;
  }
}
