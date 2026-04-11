package com.odde.doughnut.services.book;

import com.odde.doughnut.controllers.dto.AttachBookLayoutNodeRequest;
import com.odde.doughnut.controllers.dto.AttachBookLayoutRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds attach-book {@link AttachBookLayoutRequest} from a MinerU {@code content_list} array.
 * Matches {@code layout_roots_with_content_blocks} in {@code cli/python/mineru_book_outline.py}.
 */
public final class MineruContentListLayoutBuilder {

  private MineruContentListLayoutBuilder() {}

  public static AttachBookLayoutRequest buildLayout(List<?> contentList) {
    List<AttachBookLayoutNodeRequest> roots = new ArrayList<>();
    List<StackEntry> stack = new ArrayList<>();
    AttachBookLayoutNodeRequest beginningNode = null;

    for (Object el : contentList) {
      if (!(el instanceof Map<?, ?> rawMap)) {
        continue;
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> item = (Map<String, Object>) rawMap;

      Object itemType = item.get("type");
      Integer textLevel = textLevelIfTextHeading(itemType, item.get("text_level"));

      if ("text".equals(itemType) && textLevel != null && textLevel >= 1 && textLevel <= 3) {
        String title = stringOrEmpty(item.get("text")).trim();
        if (title.isEmpty()) {
          continue;
        }
        Object pageIdx = item.get("page_idx");
        Object bbox = item.get("bbox");
        if (pageIdx == null || !isValidBbox(bbox)) {
          continue;
        }
        flushBeginning(roots, beginningNode);
        beginningNode = null;

        AttachBookLayoutNodeRequest node = new AttachBookLayoutNodeRequest();
        node.setTitle(title);
        List<Map<String, Object>> cbs = new ArrayList<>();
        cbs.add(item);
        node.setContentBlocks(cbs);

        while (!stack.isEmpty() && stack.getLast().level >= textLevel) {
          stack.removeLast();
        }
        if (stack.isEmpty()) {
          roots.add(node);
        } else {
          ensureChildren(stack.getLast().node).add(node);
        }
        stack.add(new StackEntry(textLevel, node));
      } else {
        if (!stack.isEmpty()) {
          stack.getLast().node.getContentBlocks().add(item);
        } else {
          if (beginningNode == null) {
            Map<String, Object> anchorPayload = beginningAnchorPayload(item);
            Object apPage = anchorPayload.get("page_idx");
            Object apBbox = anchorPayload.get("bbox");
            if (apPage != null && isValidBbox(apBbox)) {
              Map<String, Object> synthetic = new LinkedHashMap<>();
              synthetic.put("type", "beginning_anchor");
              synthetic.putAll(anchorPayload);
              beginningNode = new AttachBookLayoutNodeRequest();
              beginningNode.setTitle("*beginning*");
              List<Map<String, Object>> bc = new ArrayList<>();
              bc.add(synthetic);
              beginningNode.setContentBlocks(bc);
            }
          }
          if (beginningNode != null) {
            beginningNode.getContentBlocks().add(item);
          }
        }
      }
    }

    if (beginningNode != null) {
      roots.add(beginningNode);
    }

    AttachBookLayoutRequest layout = new AttachBookLayoutRequest();
    layout.setRoots(roots);
    return layout;
  }

  private static void flushBeginning(
      List<AttachBookLayoutNodeRequest> roots, AttachBookLayoutNodeRequest beginningNode) {
    if (beginningNode != null) {
      roots.add(beginningNode);
    }
  }

  private static List<AttachBookLayoutNodeRequest> ensureChildren(AttachBookLayoutNodeRequest n) {
    if (n.getChildren() == null) {
      n.setChildren(new ArrayList<>());
    }
    return n.getChildren();
  }

  private static Integer textLevelIfTextHeading(Object itemType, Object textLevelObj) {
    if (!"text".equals(itemType)) {
      return null;
    }
    if (textLevelObj instanceof Number n) {
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
