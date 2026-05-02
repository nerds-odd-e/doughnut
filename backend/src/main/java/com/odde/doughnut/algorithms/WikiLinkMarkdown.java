package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Obsidian-style {@code [[wikilink]]} inner titles in markdown text (occurrence order, no dedupe).
 */
public final class WikiLinkMarkdown {

  public static final Pattern INNER_LINK_PATTERN = Pattern.compile("\\[\\[([^\\]]+)]]");

  private WikiLinkMarkdown() {}

  public static List<String> innerTitlesInOccurrenceOrder(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return List.of();
    }
    List<String> titles = new ArrayList<>();
    Matcher matcher = INNER_LINK_PATTERN.matcher(markdown);
    while (matcher.find()) {
      String t = matcher.group(1).trim();
      if (!t.isEmpty()) {
        titles.add(t);
      }
    }
    return titles;
  }
}
