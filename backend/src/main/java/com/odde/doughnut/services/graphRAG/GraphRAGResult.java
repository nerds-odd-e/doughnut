package com.odde.doughnut.services.graphRAG;

import java.util.ArrayList;
import java.util.List;

public class GraphRAGResult {
  private FocusNote focusNote;
  private List<BareNote> relatedNotes = new ArrayList<>();

  public FocusNote getFocusNote() {
    return focusNote;
  }

  public void setFocusNote(FocusNote focusNote) {
    this.focusNote = focusNote;
  }

  public List<BareNote> getRelatedNotes() {
    return relatedNotes;
  }

  public void setRelatedNotes(List<BareNote> relatedNotes) {
    this.relatedNotes = relatedNotes;
  }
}
