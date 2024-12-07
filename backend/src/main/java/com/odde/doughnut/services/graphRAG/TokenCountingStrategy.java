package com.odde.doughnut.services.graphRAG;

public interface TokenCountingStrategy {
  int estimateTokens(BareNote bareNote);
}
