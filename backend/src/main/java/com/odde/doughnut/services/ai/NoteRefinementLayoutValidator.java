package com.odde.doughnut.services.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoteRefinementLayoutValidator {
  private static final Logger logger = LoggerFactory.getLogger(NoteRefinementLayoutValidator.class);

  public static NoteRefinementLayout validOrEmpty(NoteRefinementLayout layout) {
    Optional<String> reason = firstInvalidReason(layout);
    if (reason.isPresent()) {
      logger.warn("Rejecting invalid note refinement layout: {}", reason.get());
      return NoteRefinementLayout.empty();
    }
    return layout;
  }

  public static boolean isValid(NoteRefinementLayout layout) {
    return firstInvalidReason(layout).isEmpty();
  }

  private static Optional<String> firstInvalidReason(NoteRefinementLayout layout) {
    if (layout == null) {
      return Optional.of("layout is null");
    }
    if (layout.items == null) {
      return Optional.of("layout items is null");
    }
    Set<String> ids = new HashSet<>();
    for (NoteRefinementLayoutItem item : layout.items) {
      Optional<String> itemReason = invalidReasonForItem(item, 1, ids);
      if (itemReason.isPresent()) {
        return itemReason;
      }
    }
    return Optional.empty();
  }

  private static Optional<String> invalidReasonForItem(
      NoteRefinementLayoutItem item, int depth, Set<String> ids) {
    if (item == null) {
      return Optional.of("item is null");
    }
    if (item.id == null) {
      return Optional.of("item id is null");
    }
    if (item.id.isBlank()) {
      return Optional.of("item id is blank");
    }
    if (item.text == null) {
      return Optional.of("item text is null");
    }
    if (item.text.isBlank()) {
      return Optional.of("item text is blank");
    }
    if (!ids.add(item.id)) {
      return Optional.of("duplicate item id: " + item.id);
    }
    List<NoteRefinementLayoutItem> children = item.children;
    if (children == null) {
      return Optional.of("item children is null");
    }
    if (depth > 2) {
      return Optional.of("layout exceeds maximum depth");
    }
    for (NoteRefinementLayoutItem child : children) {
      Optional<String> childReason = invalidReasonForItem(child, depth + 1, ids);
      if (childReason.isPresent()) {
        return childReason;
      }
    }
    return Optional.empty();
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
