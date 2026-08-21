package com.odde.doughnut.services.focusContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Merges related notes across focuses; first-seen wins by (notebook, title). */
public class MergedRelatedNotes {
  private final Map<String, FocusContextNote> byNotebookAndTitle = new LinkedHashMap<>();

  public void addAll(List<FocusContextNote> notes) {
    for (FocusContextNote note : notes) {
      byNotebookAndTitle.putIfAbsent(identityKey(note), note);
    }
  }

  public List<FocusContextNote> asList() {
    return new ArrayList<>(byNotebookAndTitle.values());
  }

  private static String identityKey(FocusContextNote note) {
    String notebook = note.getNotebook() != null ? note.getNotebook() : "";
    String title = note.getTitle() != null ? note.getTitle() : "";
    return notebook + "\0" + title;
  }
}
