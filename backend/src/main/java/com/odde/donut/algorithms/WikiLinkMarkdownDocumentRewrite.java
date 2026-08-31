package com.odde.donut.algorithms;

import java.util.LinkedHashSet;
import java.util.regex.Matcher;

/** Rewrites inter-note tokens inside a Markdown document. */
public final class WikiLinkMarkdownDocumentRewrite {

  private WikiLinkMarkdownDocumentRewrite() {}

  /** Converts OS-invalid characters in wiki and path-Markdown tokens. */
  public static String replaceOsInvalidCharsInAuthoredTokens(String markdown) {
    if (markdown == null || markdown.isEmpty()) {
      return markdown;
    }
    String content = markdown;
    for (String token :
        new LinkedHashSet<>(WikiLinkMarkdown.authoredTokensInOccurrenceOrder(markdown))) {
      String converted = WikiLinkMarkdownRewrite.replaceOsInvalidCharsInStoredLinkInner(token);
      if (!converted.equals(token)) {
        content = replaceWikiLinksMatchingTrimmedInner(content, token, converted);
      }
    }
    return content;
  }

  public static String replaceWikiLinksMatchingTrimmedInner(
      String markdown, String oldInnerTrimmed, String newInner) {
    if (markdown == null || markdown.isEmpty()) {
      return markdown;
    }
    if (WikiLinkMarkdown.tryParsePathMarkdownToken(oldInnerTrimmed).isPresent()
        && WikiLinkMarkdown.tryParsePathMarkdownToken(newInner).isPresent()) {
      return replacePathMarkdownMatching(markdown, oldInnerTrimmed, newInner);
    }
    Matcher matcher = WikiLinkMarkdown.INNER_LINK_PATTERN.matcher(markdown);
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

  private static String replacePathMarkdownMatching(
      String markdown, String oldTokenTrimmed, String newToken) {
    StringBuilder out = new StringBuilder();
    int last = 0;
    for (WikiLinkMarkdown.PathMarkdownOccurrence occurrence :
        WikiLinkMarkdown.pathMarkdownOccurrences(markdown)) {
      out.append(markdown, last, occurrence.start());
      out.append(occurrence.token().equals(oldTokenTrimmed) ? newToken : occurrence.token());
      last = occurrence.end();
    }
    out.append(markdown.substring(last));
    return out.toString();
  }
}
