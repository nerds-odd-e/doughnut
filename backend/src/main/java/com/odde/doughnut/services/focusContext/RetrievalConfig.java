package com.odde.doughnut.services.focusContext;

public class RetrievalConfig {
  private final int maxDepth;

  public RetrievalConfig(int maxDepth) {
    this.maxDepth = maxDepth;
  }

  public static RetrievalConfig depth1() {
    return new RetrievalConfig(1);
  }

  public int getMaxDepth() {
    return maxDepth;
  }
}
