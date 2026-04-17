package com.odde.doughnut.services.book;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.BookContentBlock;
import java.util.ArrayList;
import java.util.List;

public final class BookBlockEpubContentLocators {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private BookBlockEpubContentLocators() {}

  public static List<ContentLocator> epubContentLocators(List<BookContentBlock> orderedBlocks) {
    if (orderedBlocks == null || orderedBlocks.isEmpty()) {
      return List.of();
    }
    EpubLocator anchor = epubLocatorFromRaw(orderedBlocks.getFirst().getRawData());
    if (anchor == null) {
      return List.of();
    }
    List<ContentLocator> out = new ArrayList<>();
    out.add(anchor);
    for (int i = 1; i < orderedBlocks.size(); i++) {
      BookContentBlock cb = orderedBlocks.get(i);
      if (!BookBlockDirectContentPredicate.contributesDirectContent(cb)) {
        continue;
      }
      EpubLocator loc = epubLocatorFromRaw(cb.getRawData());
      if (loc != null) {
        out.add(loc);
      }
    }
    return List.copyOf(out);
  }

  public static String epubStartHrefFromFirstContentBlock(List<BookContentBlock> orderedBlocks) {
    if (orderedBlocks == null || orderedBlocks.isEmpty()) {
      return null;
    }
    EpubLocator loc = epubLocatorFromRaw(orderedBlocks.getFirst().getRawData());
    if (loc == null) {
      return null;
    }
    return toEpubStartHrefString(loc);
  }

  private static EpubLocator epubLocatorFromRaw(String rawData) {
    if (rawData == null || rawData.isBlank()) {
      return null;
    }
    try {
      JsonNode n = MAPPER.readTree(rawData);
      if (n == null || !n.has("href") || !n.get("href").isTextual()) {
        return null;
      }
      String href = n.get("href").asText();
      if (href.isBlank()) {
        return null;
      }
      String fragment = null;
      if (n.has("fragment") && n.get("fragment").isTextual()) {
        String f = n.get("fragment").asText();
        if (!f.isBlank()) {
          fragment = f;
        }
      }
      return new EpubLocator(href, fragment);
    } catch (Exception e) {
      return null;
    }
  }

  private static String toEpubStartHrefString(EpubLocator loc) {
    if (loc.fragment() == null || loc.fragment().isBlank()) {
      return loc.href();
    }
    return loc.href() + loc.fragment();
  }
}
