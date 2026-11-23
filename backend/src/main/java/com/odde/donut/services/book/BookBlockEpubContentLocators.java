package com.odde.donut.services.book;

import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.entities.BookContentBlock;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class BookBlockEpubContentLocators {

  private static final JsonMapper MAPPER = ObjectMapperConfig.handWrittenJsonMapper();

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

  private static EpubLocator epubLocatorFromRaw(String rawData) {
    if (rawData == null || rawData.isBlank()) {
      return null;
    }
    try {
      JsonNode n = MAPPER.readTree(rawData);
      if (n == null || !n.has("href") || !n.get("href").isString()) {
        return null;
      }
      String href = n.get("href").asString();
      if (href.isBlank()) {
        return null;
      }
      String fragment = null;
      if (n.has("fragment") && n.get("fragment").isString()) {
        String f = n.get("fragment").asString();
        if (!f.isBlank()) {
          String bare = bareEpubFragmentId(f);
          if (!bare.isEmpty()) {
            fragment = bare;
          }
        }
      }
      return new EpubLocator(href, fragment, null);
    } catch (Exception e) {
      return null;
    }
  }

  /** Strips URL fragment marker(s); EPUB wire form uses bare element ids. */
  private static String bareEpubFragmentId(String rawFragment) {
    String t = rawFragment.trim();
    while (t.startsWith("#")) {
      t = t.substring(1).trim();
    }
    return t;
  }
}
