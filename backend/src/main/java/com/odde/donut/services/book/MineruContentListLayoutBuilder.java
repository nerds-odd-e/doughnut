package com.odde.donut.services.book;

import com.odde.donut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.donut.controllers.dto.AttachBookLayoutRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds attach-book {@link AttachBookLayoutRequest} from a MinerU {@code content_list} array.
 * Canonical implementation for attach-book {@code contentList} → layout; covered by {@code
 * com.odde.donut.services.book.MineruContentListLayoutBuilderTest}.
 *
 * <p>An ordered list of {@link OutlineEntry outline entries} splits the content items: each item
 * belongs to the last entry at or before it, and items before the first entry go to {@code
 * *beginning*}. MinerU headings are one source of entries.
 */
public final class MineruContentListLayoutBuilder {

  /**
   * One block of the outline. It starts with {@code startBlock} (its locator) and holds the content
   * items from index {@code firstItem} up to the next entry's {@code firstItem}.
   */
  record OutlineEntry(int level, String title, Map<String, Object> startBlock, int firstItem) {}

  private MineruContentListLayoutBuilder() {}

  public static AttachBookLayoutRequest buildLayout(List<?> contentList) {
    List<Map<String, Object>> items = new ArrayList<>();
    List<OutlineEntry> entries = new ArrayList<>();
    for (Object el : contentList) {
      if (!(el instanceof Map<?, ?> rawMap)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> item = (Map<String, Object>) rawMap;
      Integer level = headingLevel(item);
      if (level == null) {
        items.add(item);
        continue;
      }
      String title = stringOrEmpty(item.get("text")).trim();
      if (!title.isEmpty() && item.get("page_idx") != null && isValidBbox(item.get("bbox"))) {
        entries.add(new OutlineEntry(level, title, item, items.size()));
      }
    }
    return buildLayout(entries, items);
  }

  static AttachBookLayoutRequest buildLayout(
      List<OutlineEntry> entries, List<Map<String, Object>> items) {
    List<AttachBookLayoutNodeRequest> roots = new ArrayList<>();
    int firstEntryItem = entries.isEmpty() ? items.size() : entries.getFirst().firstItem();
    AttachBookLayoutNodeRequest beginning = beginningNode(items.subList(0, firstEntryItem));
    if (beginning != null) {
      roots.add(beginning);
    }

    List<StackEntry> stack = new ArrayList<>();
    for (int i = 0; i < entries.size(); i++) {
      OutlineEntry entry = entries.get(i);
      int end = i + 1 < entries.size() ? entries.get(i + 1).firstItem() : items.size();
      AttachBookLayoutNodeRequest node =
          node(entry.title(), entry.startBlock(), items.subList(entry.firstItem(), end));

      while (!stack.isEmpty() && stack.getLast().level >= entry.level()) {
        stack.removeLast();
      }
      if (stack.isEmpty()) {
        roots.add(node);
      } else {
        ensureChildren(stack.getLast().node).add(node);
      }
      stack.add(new StackEntry(entry.level(), node));
    }

    AttachBookLayoutRequest layout = new AttachBookLayoutRequest();
    layout.setRoots(roots);
    return layout;
  }

  private static AttachBookLayoutNodeRequest beginningNode(List<Map<String, Object>> orphans) {
    for (int i = 0; i < orphans.size(); i++) {
      Map<String, Object> anchorPayload = beginningAnchorPayload(orphans.get(i));
      if (anchorPayload.get("page_idx") != null && isValidBbox(anchorPayload.get("bbox"))) {
        Map<String, Object> synthetic = new LinkedHashMap<>();
        synthetic.put("type", "beginning_anchor");
        synthetic.putAll(anchorPayload);
        return node("*beginning*", synthetic, orphans.subList(i, orphans.size()));
      }
    }
    return null;
  }

  private static AttachBookLayoutNodeRequest node(
      String title, Map<String, Object> startBlock, List<Map<String, Object>> items) {
    AttachBookLayoutNodeRequest node = new AttachBookLayoutNodeRequest();
    node.setTitle(title);
    List<Map<String, Object>> blocks = new ArrayList<>();
    blocks.add(startBlock);
    blocks.addAll(items);
    node.setContentBlocks(blocks);
    return node;
  }

  private static List<AttachBookLayoutNodeRequest> ensureChildren(AttachBookLayoutNodeRequest n) {
    if (n.getChildren() == null) {
      n.setChildren(new ArrayList<>());
    }
    return n.getChildren();
  }

  private static Integer headingLevel(Map<String, Object> item) {
    if ("text".equals(item.get("type"))
        && item.get("text_level") instanceof Number n
        && n.intValue() >= 1
        && n.intValue() <= 3) {
      return n.intValue();
    }
    return null;
  }

  private static String stringOrEmpty(Object o) {
    return o == null ? "" : String.valueOf(o);
  }

  static boolean isValidBbox(Object bbox) {
    if (!(bbox instanceof List<?> list) || list.size() != 4) {
      return false;
    }
    double x0;
    double y0;
    double x1;
    double y1;
    try {
      x0 = toFiniteDouble(list.get(0));
      y0 = toFiniteDouble(list.get(1));
      x1 = toFiniteDouble(list.get(2));
      y1 = toFiniteDouble(list.get(3));
    } catch (IllegalArgumentException e) {
      return false;
    }
    return x0 < x1 && y0 < y1;
  }

  private static double toFiniteDouble(Object o) {
    if (o instanceof Number n) {
      double v = n.doubleValue();
      if (!Double.isFinite(v)) {
        throw new IllegalArgumentException();
      }
      return v;
    }
    throw new IllegalArgumentException();
  }

  static Map<String, Object> beginningAnchorPayload(Map<String, Object> firstOrphan) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("kind", "beginning");
    Object pageIdx = firstOrphan.get("page_idx");
    if (pageIdx != null) {
      payload.put("page_idx", pageIdx);
    }
    Object bbox = firstOrphan.get("bbox");
    if (bbox instanceof List<?> list && list.size() == 4) {
      try {
        double x0 = toFiniteDouble(list.get(0));
        double y0 = toFiniteDouble(list.get(1));
        double x1 = toFiniteDouble(list.get(2));
        double y1 = toFiniteDouble(list.get(3));
        double h = y1 - y0;
        List<Double> synthetic = List.of(x0, Math.max(0.0, y0 - h), x1, y0);
        payload.put("bbox", new ArrayList<>(synthetic));
      } catch (IllegalArgumentException ignored) {
        // omit bbox
      }
    }
    return payload;
  }

  private record StackEntry(int level, AttachBookLayoutNodeRequest node) {}
}
