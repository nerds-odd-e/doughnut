package com.odde.doughnut.services.graphRAG;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class GraphRAGResult {
  private final FocusNote focusNote;
  private final List<BareNote> relatedNotes;

  public GraphRAGResult(FocusNote focusNote) {
    this.focusNote = focusNote;
    this.relatedNotes = new ArrayList<>();
  }
}
