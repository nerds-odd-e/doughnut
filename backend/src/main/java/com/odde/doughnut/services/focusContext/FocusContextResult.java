package com.odde.doughnut.services.focusContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
