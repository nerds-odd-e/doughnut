package com.odde.doughnut.services.focusContext;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class FocusContextResult {
  private final FocusContextFocusNote focusNote;
  private final List<FocusContextNote> relatedNotes;

  public FocusContextResult(FocusContextFocusNote focusNote) {
    this.focusNote = focusNote;
    this.relatedNotes = new ArrayList<>();
  }

  public void addRelatedNote(FocusContextNote note) {
    relatedNotes.add(note);
  }
}
