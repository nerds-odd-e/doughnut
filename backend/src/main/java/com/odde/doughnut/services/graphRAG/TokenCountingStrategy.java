package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public interface TokenCountingStrategy {
  int estimateTokens(Note note);
}
