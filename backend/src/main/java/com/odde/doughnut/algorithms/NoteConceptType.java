package com.odde.doughnut.algorithms;

/** Persist-time ordinary-note concept type ({@code type: Note}) for stored markdown. */
public final class NoteConceptType {

  private static final String ORDINARY_NOTE_FENCE = "---\ntype: Note\n---\n";

  private NoteConceptType() {}

  public static String ensureOrdinaryNoteType(String content) {
    if (NoteLeadingFrontmatter.splitVerbatim(content).isPresent()) {
      return content;
    }
    String body = content == null ? "" : content;
    return ORDINARY_NOTE_FENCE + body;
  }
}
