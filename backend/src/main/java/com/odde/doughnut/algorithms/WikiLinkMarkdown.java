package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inter-note link tokens in markdown: wiki {@code [[inner]]} titles and path Markdown {@code
 * [display](/folder/File.md)} (occurrence order, no dedupe).
 */
public final class WikiLinkMarkdown {

  public static final Pattern INNER_LINK_PATTERN = Pattern.compile("\\[\\[([^\\]]+)]]");

  /**
   * Notebook-relative Markdown path link {@code [display](/folder/File.md)} (leading {@code /},
   * optional {@code .md}).
   */
  private static final Pattern PATH_MARKDOWN_LINK_PATTERN =
      Pattern.compile("\\[([^\\[\\]]*)\\]\\((/[^)\\s]+)\\)");

  /**
   * Target and display segments of an authored inter-note token (first {@code |} separates wiki
   * inners; Markdown path links use display text and href).
   */
  public record WikiInnerSplit(String target, String display) {}

  private WikiLinkMarkdown() {}

  /**
   * Target and display of an authored inter-note token: Markdown path link {@code [display](/href)}
   * or wiki inner ({@link #splitInner}).
   */
  public static WikiInnerSplit splitAuthoredToken(String authored) {
    if (authored == null || authored.isEmpty()) {
      return new WikiInnerSplit("", "");
    }
    Matcher markdown = PATH_MARKDOWN_LINK_PATTERN.matcher(authored.trim());
    if (markdown.matches() && isConceptPathHref(markdown.group(2))) {
      String display = markdown.group(1);
      String href = markdown.group(2);
      if (display.trim().isEmpty()) {
        display = href;
      }
      return new WikiInnerSplit(href, display);
    }
    return splitInner(authored);
  }

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
    return !splitInner(inner).target().trim().isEmpty();
  }

  public static List<String> authoredTokensInOccurrenceOrder(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return List.of();
    }
    record Hit(int start, String token) {}
    List<Hit> hits = new ArrayList<>();
    Matcher wiki = INNER_LINK_PATTERN.matcher(markdown);
    while (wiki.find()) {
      String t = wiki.group(1).trim();
      if (!t.isEmpty()) {
        hits.add(new Hit(wiki.start(), t));
      }
    }
    Matcher pathMarkdown = PATH_MARKDOWN_LINK_PATTERN.matcher(markdown);
    while (pathMarkdown.find()) {
      if (pathMarkdown.start() > 0 && markdown.charAt(pathMarkdown.start() - 1) == '!') {
        continue;
      }
      String href = pathMarkdown.group(2);
      if (!isConceptPathHref(href)) {
        continue;
      }
      hits.add(new Hit(pathMarkdown.start(), pathMarkdown.group(0)));
    }
    hits.sort(Comparator.comparingInt(Hit::start));
    return hits.stream().map(Hit::token).toList();
  }

  private static boolean isConceptPathHref(String href) {
    if (href == null || !href.startsWith("/") || href.startsWith("//")) {
      return false;
    }
    String path = href;
    int query = path.indexOf('?');
    if (query >= 0) {
      path = path.substring(0, query);
    }
    int hash = path.indexOf('#');
    if (hash >= 0) {
      path = path.substring(0, hash);
    }
    return !path.matches("/d/n/\\d+(/.*)?")
        && !path.matches("/n/\\d+(/.*)?")
        && !path.matches("/n\\d+");
  }
}
