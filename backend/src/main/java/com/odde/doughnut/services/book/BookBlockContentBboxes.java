package com.odde.doughnut.services.book;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.BookContentBlock;
import java.util.ArrayList;
import java.util.List;

public final class BookBlockContentBboxes {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private BookBlockContentBboxes() {}

  public static List<BookBlockContentBboxItem> fromOrderedBlocks(
      List<BookContentBlock> orderedBlocks) {
    if (orderedBlocks == null || orderedBlocks.isEmpty()) {
      return List.of();
    }
    List<BookBlockContentBboxItem> out = new ArrayList<>();
    for (BookContentBlock cb : orderedBlocks) {
      if (cb == null) {
        continue;
      }
      BookBlockContentBboxItem item = fromRaw(cb.getRawData());
      if (item != null) {
        out.add(item);
      }
    }
    return List.copyOf(out);
  }

  private static BookBlockContentBboxItem fromRaw(String rawData) {
    if (rawData == null || rawData.isBlank()) {
      return null;
    }
    try {
      JsonNode n = MAPPER.readTree(rawData);
      JsonNode pageIdxNode = n.get("page_idx");
      if (pageIdxNode == null || pageIdxNode.isNull() || !pageIdxNode.isInt()) {
        return null;
      }
      int pageIndex = pageIdxNode.intValue();
      if (pageIndex < 0) {
        return null;
      }
      JsonNode bboxNode = n.get("bbox");
      if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() != 4) {
        return null;
      }
      List<Double> bbox = new ArrayList<>(4);
      for (JsonNode c : bboxNode) {
        if (c == null || !c.isNumber()) {
          return null;
        }
        double v = c.doubleValue();
        if (!Double.isFinite(v)) {
          return null;
        }
        bbox.add(v);
      }
      double x0 = bbox.get(0);
      double y0 = bbox.get(1);
      double x1 = bbox.get(2);
      double y1 = bbox.get(3);
      if (x0 >= x1 || y0 >= y1) {
        return null;
      }
      return new BookBlockContentBboxItem(pageIndex, List.copyOf(bbox));
    } catch (Exception e) {
      return null;
    }
  }
}
