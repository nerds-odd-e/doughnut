package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.graphRAG.GraphRAGResult;

public interface GraphRAGService {
  GraphRAGResult retrieve(Note focusNote, int tokenBudget);
}
