package com.odde.doughnut.services.ai;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoteRefinementLayoutValidator {
  public static NoteRefinementLayout validOrEmpty(NoteRefinementLayout layout) {
    return isValid(layout) ? layout : NoteRefinementLayout.empty();
  }

  public static boolean isValid(NoteRefinementLayout layout) {
    if (layout == null || layout.items == null) {
      return false;
    }
    Set<String> ids = new HashSet<>();
    return layout.items.stream().allMatch(item -> isValidItem(item, 1, ids));
  }

  private static boolean isValidItem(NoteRefinementLayoutItem item, int depth, Set<String> ids) {
    if (item == null
        || item.id == null
        || item.id.isBlank()
        || item.text == null
        || item.text.isBlank()
        || !ids.add(item.id)) {
      return false;
    }
    List<NoteRefinementLayoutItem> children = item.children;
    if (children == null || depth > 2) {
      return false;
    }
    return children.stream().allMatch(child -> isValidItem(child, depth + 1, ids));
  }
}
