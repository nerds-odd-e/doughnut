package com.odde.doughnut.services.book;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.BookContentBlock;

/**
 * Direct-content predicate for reading-record heuristics: MinerU (PDF) exclusions plus EPUB aside
 * exclusions for {@code note} / {@code sidebar} semantics.
 */
public final class BookBlockDirectContentPredicate {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private BookBlockDirectContentPredicate() {}

  /**
   * Blocks that count as direct reading content. PDF: excludes header, footer, {@code page_*}, and
   * {@code text} with {@code text_level} 1–3. EPUB-shaped payloads (see {@link
   * #isEpubShapedContentPayload}): excludes types {@code note} / {@code sidebar} and any block
   * whose {@code rawData} carries {@code epub_semantic} {@code note} or {@code sidebar}.
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
    JsonNode raw = parseRaw(cb.getRawData());
    if ("text".equals(t)) {
      Integer level = textLevelFromNode(raw);
      if (level != null && level >= 1 && level <= 3) {
        return false;
      }
    }
    if (isEpubShapedContentPayload(raw)) {
      if ("note".equals(t) || "sidebar".equals(t)) {
        return false;
      }
      if (isEpubAsideSemantic(raw)) {
        return false;
      }
    }
    return true;
  }

  static Integer textLevelFromRaw(String rawData) {
    return textLevelFromNode(parseRaw(rawData));
  }

  private static JsonNode parseRaw(String rawData) {
    if (rawData == null || rawData.isBlank()) {
      return null;
    }
    try {
      return MAPPER.readTree(rawData);
    } catch (Exception e) {
      return null;
    }
  }

  private static Integer textLevelFromNode(JsonNode n) {
    if (n == null) {
      return null;
    }
    JsonNode tl = n.get("text_level");
    if (tl == null || tl.isNull() || !tl.isNumber()) {
      return null;
    }
    return tl.intValue();
  }

  /** EPUB import payloads carry spine {@code href} and {@code fragment} in {@code rawData}. */
  private static boolean isEpubShapedContentPayload(JsonNode n) {
    if (n == null || !n.has("href") || !n.has("fragment")) {
      return false;
    }
    JsonNode href = n.get("href");
    JsonNode fragment = n.get("fragment");
    return href != null && href.isTextual() && fragment != null && fragment.isTextual();
  }

  private static boolean isEpubAsideSemantic(JsonNode n) {
    JsonNode sem = n.get("epub_semantic");
    if (sem == null || !sem.isTextual()) {
      return false;
    }
    String v = sem.asText();
    return "note".equals(v) || "sidebar".equals(v);
  }
}
