package com.odde.donut.algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Wiki-link tokens in markdown: {@code [[inner]]} titles in occurrence order. */
public final class WikiLinkMarkdown {

  public static final Pattern INNER_LINK_PATTERN = Pattern.compile("\\[\\[([^\\]]+)]]");

  /**
   * Portable path and display segments of a wiki-link inner (first {@code |} separates target from
   * display).
   */
  public record WikiInnerSplit(PortablePath portablePath, String displayText) {}

  private WikiLinkMarkdown() {}

  /**
   * Splits wiki link inner text on the first {@code |}. Empty right-hand side is treated as no pipe
   * (display equals target).
   */
  public static WikiInnerSplit splitInner(String rawBetweenBrackets) {
    if (rawBetweenBrackets == null || rawBetweenBrackets.isEmpty()) {
      return new WikiInnerSplit(PortablePath.parse(""), "");
    }
    int i = rawBetweenBrackets.indexOf('|');
    if (i == -1) {
      return new WikiInnerSplit(PortablePath.parse(rawBetweenBrackets), rawBetweenBrackets);
    }
    String target = rawBetweenBrackets.substring(0, i);
    String display = rawBetweenBrackets.substring(i + 1);
    if (display.trim().isEmpty()) {
      return new WikiInnerSplit(PortablePath.parse(target), target);
    }
    return new WikiInnerSplit(PortablePath.parse(target), display);
  }

  /**
   * True when {@code trimmed} is exactly one well-formed {@code [[target]]} or {@code
   * [[target|display]]} token with a non-empty target.
   */
  public static boolean isWellFormedWholeLinkToken(String trimmed) {
    Matcher matcher = INNER_LINK_PATTERN.matcher(trimmed);
    if (!matcher.matches()) {
      return false;
    }
    String inner = matcher.group(1).trim();
    if (inner.isEmpty()) {
      return false;
    }
    return !splitInner(inner).portablePath().format().trim().isEmpty();
  }

  public static List<String> authoredTokensInOccurrenceOrder(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return List.of();
    }
    List<String> tokens = new ArrayList<>();
    Matcher wiki = INNER_LINK_PATTERN.matcher(markdown);
    while (wiki.find()) {
      String t = wiki.group(1).trim();
      if (!t.isEmpty()) {
        tokens.add(t);
      }
    }
    return tokens;
  }

  /**
   * First-occurrence unique authored tokens. Note-target folding matches alias lookup; encoded
   * {@code #prop:} keys stay case-sensitive.
   */
  public static List<String> uniqueAuthoredTokensPreserveOrder(List<String> titles) {
    List<String> out = new ArrayList<>();
    Set<String> seenDedupeKeys = new HashSet<>();
    for (String t : titles) {
      if (seenDedupeKeys.add(authoredTokenDedupeKey(t))) {
        out.add(t);
      }
    }
    return List.copyOf(out);
  }

  private static String authoredTokenDedupeKey(String token) {
    WikiInnerSplit split = splitInner(token);
    PortablePath portablePath = split.portablePath();
    if (!portablePath.hasPropertySuffix()) {
      return FrontmatterAliases.normalizedLookupKey(token);
    }
    String folded =
        portablePath.mapQualifiedNotePortion(FrontmatterAliases::normalizedLookupKey).format();
    if (split.displayText().equals(portablePath.format())) {
      return folded;
    }
    return folded + "|" + FrontmatterAliases.normalizedLookupKey(split.displayText());
  }
}
