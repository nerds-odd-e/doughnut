package com.odde.doughnut.services.book;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.BookContentBlock;
import java.util.List;

/** MinerU direct-content predicate for reading-record heuristics; see phase 3 plan §5. */
public final class BookBlockDirectContentPredicate {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private BookBlockDirectContentPredicate() {}

  public static boolean hasDirectContent(List<BookContentBlock> orderedBlocks) {
    if (orderedBlocks == null || orderedBlocks.isEmpty()) {
      return false;
    }
    for (BookContentBlock cb : orderedBlocks) {
      if (cb == null) {
        continue;
      }
      String t = cb.getType();
      if (t == null || t.isEmpty()) {
        continue;
      }
      if ("header".equals(t) || "footer".equals(t) || t.startsWith("page_")) {
        continue;
      }
      if ("table".equals(t) || "image".equals(t)) {
        return true;
      }
      if ("text".equals(t)) {
        Integer level = textLevelFromRaw(cb.getRawData());
        if (level != null && level >= 1 && level <= 3) {
          continue;
        }
        return true;
      }
    }
    return false;
  }

  static Integer textLevelFromRaw(String rawData) {
    if (rawData == null || rawData.isBlank()) {
      return null;
    }
    try {
      JsonNode n = MAPPER.readTree(rawData);
      JsonNode tl = n.get("text_level");
      if (tl == null || tl.isNull() || !tl.isNumber()) {
        return null;
      }
      return tl.intValue();
    } catch (Exception e) {
      return null;
    }
  }
}
