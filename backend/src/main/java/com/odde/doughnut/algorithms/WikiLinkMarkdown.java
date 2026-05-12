package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Double-bracket {@code [[wikilink]]} inner titles in markdown text (occurrence order, no dedupe).
 */
public final class WikiLinkMarkdown {

  public static final Pattern INNER_LINK_PATTERN = Pattern.compile("\\[\\[([^\\]]+)]]");

  /**
   * Target and display segments of {@code [[inner]]} (first {@code |} separates; mirrors frontend).
   */
  public record WikiInnerSplit(String target, String display) {}

  private WikiLinkMarkdown() {}

  /**
   * Splits wiki link inner text on the first {@code |}. Empty right-hand side is treated as no pipe
   * (display equals target).
   */
  public static WikiInnerSplit splitInner(String rawBetweenBrackets) {
    if (rawBetweenBrackets == null || rawBetweenBrackets.isEmpty()) {
      return new WikiInnerSplit("", "");
    }
    int i = rawBetweenBrackets.indexOf('|');
    if (i == -1) {
      return new WikiInnerSplit(rawBetweenBrackets, rawBetweenBrackets);
    }
    String target = rawBetweenBrackets.substring(0, i);
    String display = rawBetweenBrackets.substring(i + 1);
    if (display.trim().isEmpty()) {
      return new WikiInnerSplit(target, target);
    }
    return new WikiInnerSplit(target, display);
  }

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

  public static String newInnerForUpdateVisibleText(String storedLinkInner, String newNoteTitle) {
    return newInnerWithHandling(storedLinkInner, newNoteTitle, false);
  }

  public static String newInnerForKeepVisibleText(String storedLinkInner, String newNoteTitle) {
    return newInnerWithHandling(storedLinkInner, newNoteTitle, true);
  }

  private static String newInnerWithHandling(
      String storedLinkInner, String newNoteTitle, boolean keepVisibleText) {
    if (newNoteTitle == null) {
      throw new IllegalArgumentException("newNoteTitle");
    }
    if (storedLinkInner == null || storedLinkInner.isEmpty()) {
      return newNoteTitle;
    }
    int pipeIdx = storedLinkInner.indexOf('|');
    String rawTargetPart = pipeIdx == -1 ? storedLinkInner : storedLinkInner.substring(0, pipeIdx);
    String newTargetToken =
        replaceUnqualifiedOrQualifiedNoteTitle(rawTargetPart.trim(), newNoteTitle.trim());
    if (pipeIdx == -1) {
      return keepVisibleText ? newTargetToken + "|" + storedLinkInner.trim() : newTargetToken;
    }
    String rawDisplay = storedLinkInner.substring(pipeIdx + 1);
    if (rawDisplay.trim().isEmpty()) {
      return keepVisibleText ? newTargetToken + "|" + rawTargetPart.trim() : newTargetToken;
    }
    return newTargetToken + "|" + rawDisplay;
  }

  public static String replaceWikiLinksMatchingTrimmedInner(
      String markdown, String oldInnerTrimmed, String newInner) {
    if (markdown == null || markdown.isEmpty()) {
      return markdown;
    }
    Matcher matcher = INNER_LINK_PATTERN.matcher(markdown);
    StringBuilder out = new StringBuilder();
    int last = 0;
    while (matcher.find()) {
      out.append(markdown, last, matcher.start());
      String innerTrimmed = matcher.group(1).trim();
      if (innerTrimmed.equals(oldInnerTrimmed)) {
        out.append("[[").append(newInner).append("]]");
      } else {
        out.append(matcher.group(0));
      }
      last = matcher.end();
    }
    out.append(markdown.substring(last));
    return out.toString();
  }

  private static String replaceUnqualifiedOrQualifiedNoteTitle(
      String targetToken, String newTitle) {
    int colon = targetToken.indexOf(':');
    if (colon > 0 && colon < targetToken.length() - 1) {
      String nb = targetToken.substring(0, colon).trim();
      if (!nb.isEmpty()) {
        return nb + ":" + newTitle;
      }
    }
    return newTitle;
  }
}
