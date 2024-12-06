package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class OneTokenPerNoteStrategy implements TokenCountingStrategy {
  @Override
  public int estimateTokens(Note note) {
    return 1;
  }
}
