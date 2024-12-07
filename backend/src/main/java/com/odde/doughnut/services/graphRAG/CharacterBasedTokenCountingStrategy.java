package com.odde.doughnut.services.graphRAG;

public class CharacterBasedTokenCountingStrategy implements TokenCountingStrategy {
  @Override
  public int estimateTokens(BareNote bareNote) {
    String details = bareNote.getDetails();
    if (details == null) return 1;
    return details.length() / 4 + 1;
  }
}
