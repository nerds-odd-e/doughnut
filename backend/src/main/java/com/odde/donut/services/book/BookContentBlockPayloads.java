package com.odde.donut.services.book;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.odde.donut.entities.BookBlockTitleLimits;
import com.odde.donut.entities.BookContentBlock;

final class BookContentBlockPayloads {

  private BookContentBlockPayloads() {}

  static String stripTextLevel(ObjectMapper objectMapper, String rawData) {
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
      ObjectMapper objectMapper, BookContentBlock firstMoved) {
    String raw = firstMoved.getRawData();
    if (raw == null || raw.isBlank()) {
      return "Untitled";
    }
    try {
      JsonNode n = objectMapper.readTree(raw);
      JsonNode text = n.get("text");
      if (text != null && text.isTextual()) {
        String t = BookService.trimmedMax(text.asText(), BookBlockTitleLimits.STRUCTURAL_MAX_CHARS);
        if (!t.isEmpty()) {
          return t;
        }
      }
    } catch (JsonProcessingException e) {
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
