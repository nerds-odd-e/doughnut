package com.odde.doughnut.services.focusContext;

import com.odde.doughnut.entities.Note;

public class FocusContextWikiUri {
  public static String of(Note note) {
    String title = note.getTitle() != null ? note.getTitle() : "";
    if (note.getNotebook() == null) {
      return "[[" + title + "]]";
    }
    return "[[" + note.getNotebook().getName() + ": " + title + "]]";
  }

  public static String ofFocusNote(Note note) {
    String title = note.getTitle() != null ? note.getTitle() : "";
    return "[[" + title + "]]";
  }
}
