package com.odde.doughnut.services.graphRAG;

import java.util.List;

public class GraphRAGResult {
  public final FocusNote focusNote;
  public final List<BareNote> relatedNotes;

  public GraphRAGResult(FocusNote focusNote, List<BareNote> relatedNotes) {
    this.focusNote = focusNote;
    this.relatedNotes = relatedNotes;
  }
}
