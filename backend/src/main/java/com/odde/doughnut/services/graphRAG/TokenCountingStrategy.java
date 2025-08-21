package com.odde.doughnut.services.graphRAG;

public interface TokenCountingStrategy {
  int estimateTokens(BareNote bareNote);

  /**
   * Truncate text to fit within the approximate token budget defined by maxTokens. Implementations
   * may choose different heuristics. Returning the original text is acceptable for implementations
   * that do not perform truncation.
   */
  String truncateByApproxTokens(String text, int maxTokens);
}
