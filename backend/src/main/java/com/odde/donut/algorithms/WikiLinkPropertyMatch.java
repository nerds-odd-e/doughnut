package com.odde.donut.algorithms;

/**
 * A wiki-link token with a {@code #prop:} suffix is live only when the target note's leading
 * frontmatter has that decoded exact property key (ADR 0004).
 */
public final class WikiLinkPropertyMatch {

  private WikiLinkPropertyMatch() {}

  public static boolean matchesTargetNoteContent(String token, String targetNoteContent) {
    WikiLinkAuthoredTarget authored =
        WikiLinkAuthoredTarget.parse(WikiLinkMarkdown.splitAuthoredToken(token).target());
    if (!authored.hasPropertySuffix()) {
      return true;
    }
    return authored
        .decodedPropertyKey()
        .filter(key -> noteHasExactPropertyKey(targetNoteContent, key))
        .isPresent();
  }

  private static boolean noteHasExactPropertyKey(String content, String propertyKey) {
    return NoteContentMarkdown.splitLeadingFrontmatter(content == null ? "" : content)
        .map(lf -> lf.frontmatter().keys().contains(propertyKey))
        .orElse(false);
  }
}
