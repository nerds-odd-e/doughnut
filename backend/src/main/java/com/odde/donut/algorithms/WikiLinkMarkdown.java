package com.odde.donut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Wiki-link tokens in markdown: {@code [[inner]]} titles in occurrence order. */
public final class WikiLinkMarkdown {

  public static final Pattern INNER_LINK_PATTERN = Pattern.compile("\\[\\[([^\\]]+)]]");

  /**
   * Portable path and display segments of a wiki-link inner (first unescaped {@code |} separates
   * target from display).
   */
  public record WikiInnerSplit(
      PortablePath portablePath,
      String displayText,
      String decodedTarget,
      String rawInner,
      String rawTarget,
      String rawDisplay,
      boolean hasDisplaySeparator) {

    String rewriteTarget(UnaryOperator<String> targetTransform) {
      String trimmedTarget = decodedTarget().trim();
      String transformedTarget = targetTransform.apply(trimmedTarget);
      if (transformedTarget.equals(trimmedTarget)) {
        return rawInner;
      }
      String authoredTarget = escape(transformedTarget);
      return hasDisplaySeparator ? authoredTarget + "|" + rawDisplay : authoredTarget;
    }

    String rewriteTargetKeepingVisible(UnaryOperator<String> targetTransform) {
      String trimmedTarget = decodedTarget().trim();
      String transformedTarget = targetTransform.apply(trimmedTarget);
      if (transformedTarget.equals(trimmedTarget)) {
        return rawInner;
      }
      String visibleText =
          hasDisplaySeparator && !rawDisplay.trim().isEmpty() ? rawDisplay : escape(trimmedTarget);
      return escape(transformedTarget) + "|" + visibleText;
    }

    String rewriteNoteTitle(String newNoteTitle, boolean keepVisibleText) {
      String trimmedTarget = decodedTarget().trim();
      String transformedTarget = PortablePath.replaceNoteTitle(trimmedTarget, newNoteTitle.trim());
      if (transformedTarget.equals(trimmedTarget)) {
        return rawInner;
      }
      if (hasDisplaySeparator && !rawDisplay.trim().isEmpty()) {
        return escape(transformedTarget) + "|" + rawDisplay;
      }
      return keepVisibleText
          ? escape(transformedTarget) + "|" + escape(trimmedTarget)
          : escape(transformedTarget);
    }
  }

  private WikiLinkMarkdown() {}

  /**
   * Decodes wiki escapes once and splits on the first unescaped {@code |}. Empty right-hand side is
   * treated as no pipe (display equals target).
   */
  public static WikiInnerSplit splitInner(String rawBetweenBrackets) {
    if (rawBetweenBrackets == null || rawBetweenBrackets.isEmpty()) {
      return new WikiInnerSplit(PortablePath.parse(""), "", "", rawBetweenBrackets, "", "", false);
    }
    StringBuilder target = new StringBuilder();
    StringBuilder display = new StringBuilder();
    StringBuilder current = target;
    int separator = -1;
    for (int i = 0; i < rawBetweenBrackets.length(); i++) {
      char c = rawBetweenBrackets.charAt(i);
      if (c == '\\' && i + 1 < rawBetweenBrackets.length()) {
        char next = rawBetweenBrackets.charAt(i + 1);
        if (next == '\\' || next == '|') {
          current.append(next);
          i++;
          continue;
        }
      }
      if (c == '|' && separator == -1) {
        separator = i;
        current = display;
      } else {
        current.append(c);
      }
    }
    if (separator == -1) {
      String decodedTarget = target.toString();
      return new WikiInnerSplit(
          PortablePath.parse(decodedTarget),
          decodedTarget,
          decodedTarget,
          rawBetweenBrackets,
          rawBetweenBrackets,
          "",
          false);
    }
    String decodedTarget = target.toString();
    String decodedDisplay = display.toString();
    String rawTarget = rawBetweenBrackets.substring(0, separator);
    String rawDisplay = rawBetweenBrackets.substring(separator + 1);
    if (decodedDisplay.trim().isEmpty()) {
      return new WikiInnerSplit(
          PortablePath.parse(decodedTarget),
          decodedTarget,
          decodedTarget,
          rawBetweenBrackets,
          rawTarget,
          rawDisplay,
          true);
    }
    return new WikiInnerSplit(
        PortablePath.parse(decodedTarget),
        decodedDisplay,
        decodedTarget,
        rawBetweenBrackets,
        rawTarget,
        rawDisplay,
        true);
  }

  static String escape(String decoded) {
    return decoded.replace("\\", "\\\\").replace("|", "\\|");
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
   * Note-target folding for wiki-token uniqueness; encoded {@code #prop:} keys stay case-sensitive.
   */
  static String authoredTokenDedupeKey(String token) {
    WikiInnerSplit split = splitInner(token);
    PortablePath portablePath = split.portablePath();
    String folded =
        portablePath.hasPropertySuffix()
            ? portablePath.mapQualifiedNotePortion(FrontmatterAliases::normalizedLookupKey).format()
            : FrontmatterAliases.normalizedLookupKey(portablePath.format());
    if (split.displayText().equals(portablePath.format())) {
      return folded;
    }
    return folded + "|" + FrontmatterAliases.normalizedLookupKey(split.displayText());
  }
}
