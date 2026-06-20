package com.odde.doughnut.services.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

  public static List<NoteRefinementLayoutItem> selectedItems(
      NoteRefinementLayout layout, List<String> selectedItemIds) {
    if (layout == null
        || layout.items == null
        || selectedItemIds == null
        || selectedItemIds.isEmpty()) {
      return List.of();
    }
    Map<String, NoteRefinementLayoutItem> itemsById = indexItems(layout.items);
    List<NoteRefinementLayoutItem> selected = new ArrayList<>();
    for (String selectedId : selectedItemIds) {
      NoteRefinementLayoutItem item = itemsById.get(selectedId);
      if (item == null) {
        return List.of();
      }
      selected.add(item);
    }
    return selected;
  }

  private static Map<String, NoteRefinementLayoutItem> indexItems(
      List<NoteRefinementLayoutItem> items) {
    Map<String, NoteRefinementLayoutItem> itemsById = new HashMap<>();
    for (NoteRefinementLayoutItem item : items) {
      indexItem(item, itemsById);
    }
    return itemsById;
  }

  private static void indexItem(
      NoteRefinementLayoutItem item, Map<String, NoteRefinementLayoutItem> itemsById) {
    if (item == null || item.id == null) {
      return;
    }
    itemsById.put(item.id, item);
    if (item.children != null) {
      item.children.forEach(child -> indexItem(child, itemsById));
    }
  }
}
