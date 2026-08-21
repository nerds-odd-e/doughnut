package com.odde.doughnut.services.focusContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Merges related notes across focuses by (notebook, title); first-seen wins. */
public class MergedRelatedNotes {
  private final Set<String> claimedKeys = new HashSet<>();
  private final List<FocusContextNote> notes = new ArrayList<>();

  public void exclude(String notebook, String title) {
    claimedKeys.add(identityKey(notebook, title));
  }

  public void addAll(List<FocusContextNote> notesToAdd) {
    for (FocusContextNote note : notesToAdd) {
      if (claimedKeys.add(identityKey(note))) {
        notes.add(note);
      }
    }
  }

  public List<FocusContextNote> asList() {
    return new ArrayList<>(notes);
  }

  private static String identityKey(FocusContextNote note) {
    return identityKey(note.getNotebook(), note.getTitle());
  }

  private static String identityKey(String notebook, String title) {
    String notebookPart = notebook != null ? notebook : "";
    String titlePart = title != null ? title : "";
    return notebookPart + "\0" + titlePart;
  }
}
