package com.odde.donut.services.book;

import com.odde.donut.entities.BookBlockTitleLimits;
import com.odde.donut.entities.BookContentBlock;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

final class BookContentBlockPayloads {

  private BookContentBlockPayloads() {}

  static String stripTextLevel(JsonMapper objectMapper, String rawData) {
    if (rawData == null) {
      return rawData;
    }
    try {
      ObjectNode node = (ObjectNode) objectMapper.readTree(rawData);
      node.remove("text_level");
      return objectMapper.writeValueAsString(node);
    } catch (Exception e) {
      return rawData;
    }
  }

  static String structuralTitleFromFirstMoved(
      JsonMapper objectMapper, BookContentBlock firstMoved) {
    String raw = firstMoved.getRawData();
    if (raw == null || raw.isBlank()) {
      return "Untitled";
    }
    try {
      JsonNode n = objectMapper.readTree(raw);
      JsonNode text = n.get("text");
      if (text != null && text.isString()) {
        String t =
            BookService.trimmedMax(text.asString(), BookBlockTitleLimits.STRUCTURAL_MAX_CHARS);
        if (!t.isEmpty()) {
          return t;
        }
      }
    } catch (JacksonException e) {
      return "Untitled";
    }
    return "Untitled";
  }

  static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
