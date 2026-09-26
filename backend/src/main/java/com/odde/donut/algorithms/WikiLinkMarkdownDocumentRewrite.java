package com.odde.donut.algorithms;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * Rewrites wiki Portable-path tokens inside a Markdown document. Ordinary Markdown links, including
 * note-ID URLs, are left unchanged.
 */
public final class WikiLinkMarkdownDocumentRewrite {

  private WikiLinkMarkdownDocumentRewrite() {}

  /** Converts OS-invalid characters in wiki tokens. */
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
    Optional<NoteLeadingFrontmatter.VerbatimSplit> frontmatter =
        NoteLeadingFrontmatter.splitVerbatim(markdown);
    if (frontmatter.isEmpty()) {
      return replaceInMarkdown(markdown, oldInnerTrimmed, newInner);
    }
    NoteLeadingFrontmatter.VerbatimSplit split = frontmatter.get();
    String yaml =
        FrontmatterInPlaceEdit.rewriteSupportedValues(
            split.yamlRaw(), value -> replaceInMarkdown(value, oldInnerTrimmed, newInner));
    String body = replaceInMarkdown(split.body(), oldInnerTrimmed, newInner);
    if (yaml.equals(split.yamlRaw()) && body.equals(split.body())) {
      return markdown;
    }
    return split.rebuild(yaml, body);
  }

  private static String replaceInMarkdown(
      String markdown, String oldInnerTrimmed, String newInner) {
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
}
