package com.odde.doughnut.services.graphRAG;

import com.odde.doughnut.entities.Note;

public class GraphNoteWikiUri {
  public static String of(Note note, boolean isFocusNote) {
    String title = note.getTitle() != null ? note.getTitle() : "";
    if (isFocusNote || note.getNotebook() == null) {
      return "[[" + title + "]]";
    }
    String notebookName = note.getNotebook().getName();
    return "[[" + notebookName + ": " + title + "]]";
  }
}
