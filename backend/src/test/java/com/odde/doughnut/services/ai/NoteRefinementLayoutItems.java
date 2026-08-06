package com.odde.doughnut.services.ai;

import java.util.List;

/** Test fixtures for {@link NoteRefinementLayoutItem} construction. */
public final class NoteRefinementLayoutItems {
  private NoteRefinementLayoutItems() {}

  public static NoteRefinementLayoutItem leaf(String id, String text) {
    return leaf(id, text, false, false);
  }

  public static NoteRefinementLayoutItem leaf(
      String id, String text, boolean alreadyExtracted, boolean ledToQuestion) {
    return new NoteRefinementLayoutItem(id, text, alreadyExtracted, ledToQuestion, List.of());
  }

  public static NoteRefinementLayoutItem parent(
      String id, String text, List<NoteRefinementLayoutItem> children) {
    return new NoteRefinementLayoutItem(id, text, false, false, children);
  }
}
