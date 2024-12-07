package com.odde.doughnut.services.graphRAG;

public class OneTokenPerNoteStrategy implements TokenCountingStrategy {
  @Override
  public int estimateTokens(BareNote bareNote) {
    return 1;
  }
}
