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
    if (orderedBlocks == null || orderedBlocks.size() <= 1) {
      return false;
    }
    for (int i = 1; i < orderedBlocks.size(); i++) {
      if (contributesDirectContent(orderedBlocks.get(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * MinerU blocks that count as direct reading content. Excludes header, footer, {@code page_*},
   * and {@code text} with {@code text_level} 1–3. Other types default to counting as direct
   * content.
   */
  public static boolean contributesDirectContent(BookContentBlock cb) {
    if (cb == null) {
      return false;
    }
    String t = cb.getType();
    if (t == null || t.isEmpty()) {
      return false;
    }
    if ("header".equals(t) || "footer".equals(t) || t.startsWith("page_")) {
      return false;
    }
    if ("text".equals(t)) {
      Integer level = textLevelFromRaw(cb.getRawData());
      if (level != null && level >= 1 && level <= 3) {
        return false;
      }
    }
    return true;
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
